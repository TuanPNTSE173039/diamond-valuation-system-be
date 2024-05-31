package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.Customer;
import com.letitbee.diamondvaluationsystem.entity.Payment;
import com.letitbee.diamondvaluationsystem.entity.ValuationRequest;
import com.letitbee.diamondvaluationsystem.enums.RequestStatus;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.CustomerDTO;
import com.letitbee.diamondvaluationsystem.payload.PaymentDTO;
import com.letitbee.diamondvaluationsystem.payload.Response;
import com.letitbee.diamondvaluationsystem.repository.PaymentRepository;
import com.letitbee.diamondvaluationsystem.repository.ValuationRequestRepository;
import com.letitbee.diamondvaluationsystem.service.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private ModelMapper mapper;
    private PaymentRepository paymentRepository;
    private ValuationRequestRepository valuationRequestRepository;

    public PaymentServiceImpl(ModelMapper mapper, PaymentRepository paymentRepository, ValuationRequestRepository valuationRequestRepository) {
        this.mapper = mapper;
        this.paymentRepository = paymentRepository;
        this.valuationRequestRepository = valuationRequestRepository;
    }
    @Override
    public PaymentDTO createPayment(PaymentDTO paymentDTO) {
        Payment payment = mapToEntity(paymentDTO);
        payment.setPaytime(new Date());
        payment = paymentRepository.save(payment);

        ValuationRequest valuationRequest = payment.getValuationRequest();
        valuationRequest.setStatus(RequestStatus.RECEIVED);
        valuationRequestRepository.save(valuationRequest);
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