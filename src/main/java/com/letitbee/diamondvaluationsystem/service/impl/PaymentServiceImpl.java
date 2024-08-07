package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.*;
import com.letitbee.diamondvaluationsystem.enums.RequestStatus;
import com.letitbee.diamondvaluationsystem.enums.Role;
import com.letitbee.diamondvaluationsystem.exception.APIException;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.CustomerDTO;
import com.letitbee.diamondvaluationsystem.payload.PaymentDTO;
import com.letitbee.diamondvaluationsystem.payload.Response;
import com.letitbee.diamondvaluationsystem.repository.AccountRepository;
import com.letitbee.diamondvaluationsystem.repository.NotificationRepository;
import com.letitbee.diamondvaluationsystem.repository.PaymentRepository;
import com.letitbee.diamondvaluationsystem.repository.ValuationRequestRepository;
import com.letitbee.diamondvaluationsystem.service.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private ModelMapper mapper;
    private PaymentRepository paymentRepository;
    private ValuationRequestRepository valuationRequestRepository;
    private NotificationRepository notificationRepository;
    private AccountRepository accountRepository;

    public PaymentServiceImpl(ModelMapper mapper, PaymentRepository paymentRepository, ValuationRequestRepository valuationRequestRepository
    , NotificationRepository notificationRepository, AccountRepository accountRepository) {
        this.mapper = mapper;
        this.paymentRepository = paymentRepository;
        this.valuationRequestRepository = valuationRequestRepository;
        this.notificationRepository = notificationRepository;
        this.accountRepository = accountRepository;
    }
    @Override
    public PaymentDTO createPayment(PaymentDTO paymentDTO) {
        ValuationRequest valuationRequest = valuationRequestRepository.findById(
                        paymentDTO.getValuationRequestID())
                .orElseThrow(() -> new ResourceNotFoundException("Valuation Request", "id", paymentDTO.getValuationRequestID() + ""));
        if(valuationRequest.getCreationDate() == null) {
            throw new ResourceNotFoundException("Valuation Request", "id", valuationRequest.getId() + "");
        }

        if (valuationRequest.getPayment().size() >= 2) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Can't create more than 2 payments");
        }

        for (ValuationRequestDetail valuationRequestDetail : valuationRequest.getValuationRequestDetails()) {
            if(valuationRequestDetail.getSize() == 0) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Size can't be 0");
            }
        }

        Payment payment = mapToEntity(paymentDTO);

        if(valuationRequest.getPayment().isEmpty()) {
            payment.setPaytime(new Date());
            payment = paymentRepository.save(payment);
            valuationRequest.setStatus(RequestStatus.RECEIVED);
            valuationRequestRepository.save(valuationRequest);
            Notification notification = new Notification();
            notification.setAccount(accountRepository.findByRole(Role.MANAGER));
            notification.setCreationDate(new Date());
            notification.setMessage("Valuation Request #" + valuationRequest.getId() +  " has been received");
            notificationRepository.save(notification);

        } else {
            payment.setPaytime(new Date());
            payment = paymentRepository.save(payment);
            valuationRequest.setStatus(RequestStatus.FINISHED);
            valuationRequestRepository.save(valuationRequest);
            Notification notification = new Notification();
            notification.setAccount(accountRepository.findByRole(Role.MANAGER));
            notification.setCreationDate(new Date());
            notification.setMessage("Valuation Request #" + valuationRequest.getId() +  " has been finished");
            notificationRepository.save(notification);
        }
        return mapToDTO(payment);
    }

    @Override
    public Response<PaymentDTO> getAllPayment(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

//        Page<Customer> page = customerRepository.findAll(pageable);
        Page<Payment> page = paymentRepository.findAll(pageable);
        List<Payment> payments = page.getContent();

        List<PaymentDTO> paymentDTOList = payments.stream().
                map(payment -> mapToDTO(payment)).
                toList();

        Response<PaymentDTO> response = new Response<>();

        response.setContent(paymentDTOList);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;
    }



    private Payment mapToEntity(PaymentDTO paymentDTO) {
        return mapper.map(paymentDTO, Payment.class);
    }

    private PaymentDTO mapToDTO(Payment payment) {
        return mapper.map(payment, PaymentDTO.class);
    }

}
