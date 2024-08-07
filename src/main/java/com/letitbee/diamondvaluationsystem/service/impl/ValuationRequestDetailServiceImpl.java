package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.*;
import com.letitbee.diamondvaluationsystem.entity.Record;
import com.letitbee.diamondvaluationsystem.enums.*;
import com.letitbee.diamondvaluationsystem.exception.APIException;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.DiamondValuationNoteDTO;
import com.letitbee.diamondvaluationsystem.payload.Response;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestDetailDTO;
import com.letitbee.diamondvaluationsystem.repository.*;
import com.letitbee.diamondvaluationsystem.service.ValuationRequestDetailService;
import com.letitbee.diamondvaluationsystem.utils.Tools;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValuationRequestDetailServiceImpl implements ValuationRequestDetailService {

    private ModelMapper mapper;
    private ValuationRequestDetailRepository valuationRequestDetailRepository;
    private ValuationRequestRepository valuationRequestRepository;
    private DiamondValuationNoteRepository diamondValuationNoteRepository;
    private ServicePriceListRepository servicePriceListRepository;
    private DiamondMarketRepository diamondMarketRepository;
    private DiamondValuationAssignRepository diamondValuationAssignRepository;
    private DiamondValuationNoteServiceImpl diamondValuationNoteServiceImpl;
    private RecordRepository recordRepository;
    private NotificationRepository notificationRepository;
    private AccountRepository accountRepository;
    private String siteURL = "https://www.hntdiamond.store/";

    @Autowired
    private JavaMailSender javaMailSender;

    public ValuationRequestDetailServiceImpl(ModelMapper mapper,
                                             ValuationRequestDetailRepository valuationRequestDetailRepository,
                                             ValuationRequestRepository valuationRequestRepository,
                                             DiamondValuationNoteRepository diamondValuationNoteRepository,
                                             ServicePriceListRepository servicePriceListRepository,
                                             DiamondValuationAssignRepository diamondValuationAssignRepository,
                                             DiamondMarketRepository diamondMarketRepository,
                                             DiamondValuationNoteServiceImpl diamondValuationNoteServiceImpl,
                                             RecordRepository recordRepository,
                                                NotificationRepository notificationRepository,
                                                AccountRepository accountRepository
    ) {
        this.mapper = mapper;
        this.valuationRequestDetailRepository = valuationRequestDetailRepository;
        this.valuationRequestRepository = valuationRequestRepository;
        this.diamondValuationNoteRepository = diamondValuationNoteRepository;
        this.servicePriceListRepository = servicePriceListRepository;
        this.diamondValuationAssignRepository = diamondValuationAssignRepository;
        this.diamondMarketRepository = diamondMarketRepository;
        this.diamondValuationNoteServiceImpl = diamondValuationNoteServiceImpl;
        this.recordRepository = recordRepository;
        this.notificationRepository = notificationRepository;
        this.accountRepository = accountRepository;
    }

    private ValuationRequestDetail mapToEntity(ValuationRequestDetailDTO valuationRequestDetailDTO) {
        return mapper.map(valuationRequestDetailDTO, ValuationRequestDetail.class);
    }

    private ValuationRequestDetailDTO mapToDTO(ValuationRequestDetail valuationRequestDetail) {
        ValuationRequestDetailDTO valuationRequestDetailDTO = mapper.map(valuationRequestDetail, ValuationRequestDetailDTO.class);
        if (valuationRequestDetail.getDiamondValuationNote() != null
                && valuationRequestDetail.getDiamondValuationNote().getClarityCharacteristic() != null) {
            DiamondValuationNoteDTO diamondValuationNoteDTO =
                    diamondValuationNoteServiceImpl.getDiamondValuationNoteById(
                            valuationRequestDetail.getDiamondValuationNote().getId());
            valuationRequestDetailDTO.setDiamondValuationNote(diamondValuationNoteDTO);
        }
        return valuationRequestDetailDTO;
    }

    @Override
    public Response<ValuationRequestDetailDTO> getAllValuationRequestDetail(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<ValuationRequestDetail> page = valuationRequestDetailRepository.findAll(pageable);
        List<ValuationRequestDetail> valuationRequestDetails = page.getContent();

        List<ValuationRequestDetailDTO> listDTO = valuationRequestDetails.
                stream().
                map((valuationRequestDetail) -> mapToDTO(valuationRequestDetail)).toList();

        Response<ValuationRequestDetailDTO> response = new Response<>();

        response.setContent(listDTO);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;

    }

    @Override
    public ValuationRequestDetailDTO getValuationRequestDetailById(Long id) {
        ValuationRequestDetail valuationRequestDetail = valuationRequestDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation request detail", "id", id + ""));
        return mapToDTO(valuationRequestDetail);
    }

    @Override
    public ValuationRequestDetailDTO updateValuationRequestDetail(long id, ValuationRequestDetailDTO valuationRequestDetailDTO) {
        //get valuation request detail
        ValuationRequestDetail valuationRequestDetail = valuationRequestDetailRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation request detail", "id", id + ""));

        //set data to valuation request detail
        ValuationRequest valuationRequest = valuationRequestDetail.getValuationRequest();
        if(valuationRequest.getStaff() == null) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Staff must be assigned to valuation request first");
        }
        float size = (float) Math.round(valuationRequestDetailDTO.getSize() * 100) / 100;

        if(valuationRequestDetailDTO.getSize() != 0) {
            if(recordRepository.findByValuationRequestIdAndType(valuationRequest.getId(), RecordType.RECEIPT).isPresent()) {
                valuationRequestDetail.setSize(size);
            } else {
                throw new APIException(HttpStatus.BAD_REQUEST, "Receipt must be created first");
            }
        }

        if(valuationRequestDetailDTO.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.CANCEL.toString())){
            if(valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.ASSESSED.toString())
            || valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.VALUATING.toString())
            || valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.VALUATED.toString())
            || valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.APPROVED.toString())) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Cannot cancel valuation request detail");

            }
        }
        if(valuationRequestDetailDTO.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.ASSESSED.toString())){
            DiamondValuationNote diamondValuationNote = valuationRequestDetail.getDiamondValuationNote();
            if(diamondValuationNote.getCaratWeight() < 0 || diamondValuationNote.getClarity() == null || diamondValuationNote.getColor() == null
                    || diamondValuationNote.getCut() == null || diamondValuationNote.getFluorescence() == null || diamondValuationNote.getPolish() == null
                    || diamondValuationNote.getSymmetry() == null || diamondValuationNote.getShape() == null || diamondValuationNote.getDiamondOrigin() == null
                    || diamondValuationNote.getCutScore() <0 || diamondValuationNote.getClarityCharacteristicLink() == null || diamondValuationNote.getProportions() == null
                    || diamondValuationNote.getClarityCharacteristic().isEmpty()) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Diamond valuation note must be filled");
            }
        }

        //check status
        if(!(valuationRequest.getStatus().toString().equalsIgnoreCase(RequestStatus.RECEIVED.toString())
                || valuationRequest.getStatus().toString().equalsIgnoreCase(RequestStatus.VALUATING.toString()))) {
            if(valuationRequestDetailDTO.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.ASSESSING.toString())) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Request must be changed to received first");
            }
        }

        valuationRequestDetail.setStatus(valuationRequestDetailDTO.getStatus());
        valuationRequestDetail.setResultLink(valuationRequestDetailDTO.getResultLink());
        valuationRequestDetail.setCancelReason(valuationRequestDetailDTO.getCancelReason());
        valuationRequestDetail.setDiamond(valuationRequestDetailDTO.isDiamond());

        //delete diamond note when know diamond is fake
        deleteDiamondValuationNote(valuationRequestDetailDTO, valuationRequestDetail);


        //update Service Price
        if (valuationRequestDetailDTO.isDiamond()) {
            updateServicePrice(size, valuationRequestDetail);
        }

        //update valuation price base on mode
        updateValuationPriceBaseOnMode(valuationRequestDetailDTO.isMode(), valuationRequestDetail, valuationRequestDetailDTO);
        valuationRequestDetail.setMode(valuationRequestDetailDTO.isMode());

        //save to database
        valuationRequestDetail = valuationRequestDetailRepository.save(valuationRequestDetail);

        if(valuationRequestDetailDTO.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.CANCEL.toString())){
            //send notification to manager
            Notification notification = new Notification();
            notification.setAccount(accountRepository.findByRole(Role.MANAGER));
            notification.setMessage("Request detail $" + valuationRequestDetail.getId() + " in request #" + valuationRequest.getId() + " has been canceled");
            notification.setRead(false);
            notification.setCreationDate(new Date());
            notificationRepository.save(notification);
        }

        if (valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.CANCEL.toString())
                || valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.ASSESSING.toString())) {
            //change status of valuation request to valuating when status of detail is cancel or assessing
            changeValuationRequestStatusToValuating(valuationRequest);
        } else if (
                valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.ASSESSED.toString())) {
            updateDiamondValuationNote(valuationRequestDetail);//update diamond valuation note price when status id assessed
            generateCertificate(valuationRequestDetail); // generate certificate id and certificate date;
            //send notification to manager
            Notification notification = new Notification();
            notification.setAccount(accountRepository.findByRole(Role.MANAGER));
            notification.setMessage("Request detail $" + valuationRequestDetail.getId() + " in request #" + valuationRequest.getId() + " has been assessed");
            notification.setRead(false);
            notification.setCreationDate(new Date());
            notificationRepository.save(notification);
        }
        // update valuation request if valuation request detail status is cancel or assessing
        changeValuationRequestStatusToComplete(valuationRequest); //update valuation request status to complete
        //if all detail is approve or cancel
        valuationRequestDetail = valuationRequestDetailRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation request detail", "id", id + ""));

        return mapToDTO(valuationRequestDetail);
    }

    private void updateValuationRequestStatus(ValuationRequest valuationRequest, RequestStatus requestStatus) {
        valuationRequest.setStatus(requestStatus);
        valuationRequestRepository.save(valuationRequest);
    }

    private void changeValuationRequestStatusToValuating(ValuationRequest valuationRequest) {
        if (valuationRequest.getStatus().toString().equalsIgnoreCase(RequestStatus.RECEIVED.toString())) {
            RequestStatus requestStatus = RequestStatus.VALUATING;
            updateValuationRequestStatus(valuationRequest, requestStatus);
        } // update valuation request if its status is "received"
    }

    private void changeValuationRequestStatusToComplete(ValuationRequest valuationRequest) {
        Set<ValuationRequestDetail> valuationRequestDetailSet = valuationRequest.getValuationRequestDetails();

        boolean checkStatusDetail = true;
        //check status in all valuation request detail
        for (ValuationRequestDetail valuationRequestDetail : valuationRequestDetailSet) {
            if (!(valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.CANCEL.toString())
                    || valuationRequestDetail.getStatus().toString().equalsIgnoreCase(RequestDetailStatus.APPROVED.toString()))) {
                checkStatusDetail = false;
            }
        }
        if (checkStatusDetail) {
            RequestStatus requestStatus = RequestStatus.COMPLETED;
            updateValuationRequestStatus(valuationRequest, requestStatus);
            Notification notification = new Notification();
            notification.setAccount(valuationRequest.getCustomer().getAccount());
            notification.setMessage("Valuation request #" + valuationRequest.getId() + " has been completed");
            notification.setRead(false);
            notification.setCreationDate(new Date());
            notificationRepository.save(notification);
            Notification notificationConsultant = new Notification();
            notificationConsultant.setAccount(valuationRequest.getStaff().getAccount());
            notificationConsultant.setMessage("Valuation request #" + valuationRequest.getId() + " has been completed");
            notificationConsultant.setRead(false);
            notificationConsultant.setCreationDate(new Date());
            notificationRepository.save(notificationConsultant);
            try {
                sendCompleteNotiForCus(siteURL, valuationRequest.getCustomer().getAccount(), valuationRequest.getCustomer(), valuationRequest);
            } catch (MessagingException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } // update valuation request if its all detail status is cancel or approve

    }

    private void sendCompleteNotiForCus(String siteURL, Account account, Customer customer, ValuationRequest valuationRequest) throws MessagingException, UnsupportedEncodingException {
        String subject = "Your valuation request has been completed";
        String senderName = "H&T Diamond";
        // Format dates
        String creationDate = new SimpleDateFormat("yyyy-MM-dd").format(valuationRequest.getCreationDate());
        String completeDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String mailContent = "<div style=\"font-family: Arial, sans-serif; background-color: #f0f0f0;\">";
        mailContent += "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background: url('https://foreverflawlessnews.com/wp-content/uploads/2018/02/diamond.jpeg') no-repeat center center / cover; filter: blur(8px);\">";
        mailContent += "<tr>";
        mailContent += "<td align=\"center\" valign=\"top\" style=\"padding: 50px;\">";
        mailContent += "<table width=\"50%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: rgba(255, 255, 255, 0.8); border-radius: 10px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); text-align: left;\">";
        mailContent += "<tr><td style=\"padding: 20px;\">";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">Dear " + customer.getFirstName() + " " + customer.getLastName() + ",</p>";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">Name: " + customer.getFirstName() + " " + customer.getLastName() + "</p>";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">Phone: " + customer.getPhone() + "</p>";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">Address: " + customer.getAddress() + "</p>";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">RequestId: " + valuationRequest.getId() + "</p>";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">CreationDate: " + creationDate + "</p>";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">CompleteDate: " + completeDate + "</p>";
        mailContent += "<p style=\"margin: 0 0 20px; color: #000000;\">Your valuation request has been completed successfully.</p>";
        mailContent += "<p style=\"margin: 0 0 20px; color: #000000;\">Thank you for choosing H&T Diamond. You can return to our home page using the link below:</p>";
        mailContent += "<h3 style=\"margin: 0 0 20px;\"><a href=\"" + siteURL + "\" style=\"color: #0066cc; text-decoration: none;\">Click here to go to Home Page</a></h3>";
        mailContent += "<p style=\"margin: 0; color: #000000;\">Thank you,<br>The H&T Diamond Team</p>";
        mailContent += "</td></tr></table></td></tr></table></div>";

        // Send the email
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("hntdiamond@gmail.com", senderName);
        helper.setTo(account.getEmail());
        helper.setSubject(subject);
        helper.setText(mailContent, true);
        javaMailSender.send(message);
    }


    private void deleteDiamondValuationNote(ValuationRequestDetailDTO valuationRequestDetailDTO
            , ValuationRequestDetail valuationRequestDetail) {
        if (valuationRequestDetail.isDiamond() && !valuationRequestDetailDTO.isDiamond()) {
            DiamondValuationNote diamondValuationNote = valuationRequestDetail.getDiamondValuationNote();
            diamondValuationNoteRepository.delete(diamondValuationNote);
        }
    }

    private void updateServicePrice(float sizeDTO,
                                    ValuationRequestDetail valuationRequestDetail) {
        com.letitbee.diamondvaluationsystem.entity.Service service = valuationRequestDetail.getValuationRequest().getService();
        ServicePriceList servicePriceList = servicePriceListRepository.findByMinSizeLessThanEqualAndMaxSizeGreaterThanEqualAndService(sizeDTO, sizeDTO, service);
        double servicePrice = servicePriceList.getInitPrice() +
                servicePriceList.getUnitPrice() * (sizeDTO - servicePriceList.getMinSize());
        ValuationRequest valuationRequest = valuationRequestDetail.getValuationRequest();
        double totalPrice = valuationRequest.getTotalServicePrice();
        totalPrice = totalPrice + servicePrice - valuationRequestDetail.getServicePrice();

        valuationRequestDetail.setServicePrice(servicePrice);
        valuationRequest.setTotalServicePrice(totalPrice);
        valuationRequestRepository.save(valuationRequest);
    }

    private void generateCertificate(ValuationRequestDetail valuationRequestDetail) {
        DiamondValuationNote diamondValuationNote = valuationRequestDetail.getDiamondValuationNote();
        Date certificateDate = new Date();
        String certificateId = "";
        do {
            certificateId = Tools.generateId(10);
        } while (diamondValuationNoteRepository.countByCertificateId(certificateId) != 0);
        diamondValuationNote.setCertificateId(certificateId);
        diamondValuationNote.setCertificateDate(certificateDate);
        diamondValuationNoteRepository.save(diamondValuationNote);
    }

    private void updateDiamondValuationNote(ValuationRequestDetail valuationRequestDetail) {
        DiamondValuationNote diamondValuationNoteDTO = valuationRequestDetail.getDiamondValuationNote();
        List<DiamondMarket> diamondMarkets = diamondMarketRepository.findSelectedFieldsByDiamondProperties(
                diamondValuationNoteDTO.getDiamondOrigin(),
                diamondValuationNoteDTO.getCaratWeight(),
                diamondValuationNoteDTO.getColor(),
                diamondValuationNoteDTO.getClarity(),
                diamondValuationNoteDTO.getCut(),
                diamondValuationNoteDTO.getPolish(),
                diamondValuationNoteDTO.getSymmetry(),
                diamondValuationNoteDTO.getShape(),
                diamondValuationNoteDTO.getFluorescence());

        if (diamondMarkets != null && !diamondMarkets.isEmpty()) {
            //get current diamond price
            double fairPrice = 0;
            for (DiamondMarket diamondMarket : diamondMarkets) {
                fairPrice += diamondMarket.getPrice();
            }
            fairPrice = fairPrice / diamondMarkets.size();
            diamondValuationNoteDTO.setFairPrice(fairPrice);
            diamondValuationNoteDTO.setMaxPrice(diamondMarkets.get(diamondMarkets.size() - 1).getPrice());
            diamondValuationNoteDTO.setMinPrice(diamondMarkets.stream().findFirst().get().getPrice());
        }
        diamondValuationNoteRepository.save(mapper.map(diamondValuationNoteDTO, DiamondValuationNote.class));
    }

    private void updateValuationPriceBaseOnMode(boolean mode, ValuationRequestDetail valuationRequestDetail,
                                                ValuationRequestDetailDTO valuationRequestDetailDTO) {
        Set<DiamondValuationAssign> diamondValuationAssigns = valuationRequestDetail.getDiamondValuationAssigns();
        if (diamondValuationAssigns != null) {
            if (mode) {
                int i = 0;
                double valuationPrice = 0;
                for (DiamondValuationAssign diamondValuationAssign : diamondValuationAssigns) {
                    i++;
                    valuationPrice += diamondValuationAssign.getValuationPrice();
                }
                valuationPrice = valuationPrice / i;
                valuationRequestDetail.setValuationPrice(valuationPrice);
            } else {
                if (valuationRequestDetailDTO.getDiamondValuationAssign() != null) {
                    DiamondValuationAssign diamondValuationAssign =
                            diamondValuationAssignRepository.findById(valuationRequestDetailDTO.getDiamondValuationAssign().getId()).
                                    orElseThrow(() -> new ResourceNotFoundException("Diamond Valuation Assign", "id", valuationRequestDetailDTO.getDiamondValuationAssign().getId() + ""));
                    double valuationPrice = diamondValuationAssign.getValuationPrice();
                    valuationRequestDetail.setDiamondValuationAssign(diamondValuationAssign);
                    valuationRequestDetail.setValuationPrice(valuationPrice);
                }

            }
        }
    }


}
