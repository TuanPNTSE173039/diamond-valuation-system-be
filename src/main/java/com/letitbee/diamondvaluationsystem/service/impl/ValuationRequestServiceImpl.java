package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.*;
import com.letitbee.diamondvaluationsystem.enums.RequestDetailStatus;
import com.letitbee.diamondvaluationsystem.enums.RequestStatus;
import com.letitbee.diamondvaluationsystem.enums.Role;
import com.letitbee.diamondvaluationsystem.exception.APIException;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.*;
import com.letitbee.diamondvaluationsystem.repository.*;
import com.letitbee.diamondvaluationsystem.service.ValuationRequestService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.letitbee.diamondvaluationsystem.enums.RequestStatus.*;

@Service
public class ValuationRequestServiceImpl implements ValuationRequestService {
    private ValuationRequestRepository valuationRequestRepository;
    private ValuationRequestDetailRepository valuationRequestDetailRepository;
    private StaffRepository staffRepository;
    private DiamondValuationNoteRepository diamondValuationNoteRepository;
    private DiamondValuationNoteServiceImpl diamondValuationNoteServiceImpl;
    private NotificationRepository notificationRepository;
    private final ModelMapper mapper;
    private AccountRepository accountRepository;

    public ValuationRequestServiceImpl(ValuationRequestRepository valuationRequestRepository,
                                       ValuationRequestDetailRepository valuationRequestDetailRepository,
                                       StaffRepository staffRepository,
                                       DiamondValuationNoteRepository diamondValuationNoteRepository,
                                       ModelMapper mapper,
                                       DiamondValuationNoteServiceImpl diamondValuationNoteServiceImpl,
                                       NotificationRepository notificationRepository,
                                       AccountRepository accountRepository) {
        this.valuationRequestRepository = valuationRequestRepository;
        this.valuationRequestDetailRepository = valuationRequestDetailRepository;
        this.staffRepository = staffRepository;
        this.diamondValuationNoteRepository = diamondValuationNoteRepository;
        this.mapper = mapper;
        this.accountRepository = accountRepository;
        this.diamondValuationNoteServiceImpl = diamondValuationNoteServiceImpl;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Response<ValuationRequestResponse> getAllValuationRequests(int pageNo, int pageSize, String sortBy, String sortDir, Date startDate, Date endDate) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<ValuationRequest> page = valuationRequestRepository.findByCreationDateBetween(startDate, endDate, pageable);
        List<ValuationRequest> valuationRequests = page.getContent();

        List<ValuationRequestResponse> listDTO = valuationRequests.
                stream().
                map(valuationRequest -> this.mapToResponse(valuationRequest, ValuationRequestResponse.class)).collect(Collectors.toList());

        Response<ValuationRequestResponse> response = new Response<>();

        response.setContent(listDTO);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;
    }


    @Override
    public ValuationRequestDTO getValuationRequestById(Long id) {
        ValuationRequest valuationRequest = valuationRequestRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Valuation request", "id", id + ""));
        return mapToDTO(valuationRequest);
    }

    @Override
    public ValuationRequestDTO createValuationRequest(ValuationRequestDTO valuationRequestDto) {
        ValuationRequest valuationRequest = mapToEntity(valuationRequestDto);
        valuationRequest.setStatus(RequestStatus.PENDING);
        valuationRequest.setCreationDate(new Date());
        valuationRequest = valuationRequestRepository.save(valuationRequest);
        for (int i = 0; i < valuationRequest.getDiamondAmount(); i++) {
            ValuationRequestDetail valuationRequestDetail = new ValuationRequestDetail();
            valuationRequestDetail.setValuationRequest(valuationRequest);
            valuationRequestDetail.setStatus(RequestDetailStatus.PENDING);
            valuationRequestDetail.setDiamond(true);
            valuationRequestDetail = valuationRequestDetailRepository.save(valuationRequestDetail);

            DiamondValuationNote diamondValuationNote = new DiamondValuationNote();
            diamondValuationNote.setValuationRequestDetail(valuationRequestDetail);
            diamondValuationNoteRepository.save(diamondValuationNote);
        }
        Notification notificationDTO = new Notification();
        notificationDTO.setAccount(accountRepository.findByRole(Role.MANAGER));
        notificationDTO.setRead(false);
        notificationDTO.setCreationDate(new Date());
        notificationDTO.setMessage("New valuation request number #" + valuationRequest.getId() + " has been created");
        notificationRepository.save(notificationDTO);
        return mapToDTO(valuationRequest);
    }

    @Override
    public ValuationRequestDTO updateValuationRequest(long id, ValuationRequestDTO valuationRequestDTO) {
        ValuationRequest valuationRequest = valuationRequestRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation Request", "id", id + ""));
        //get staff
        Staff staff = staffRepository.findById(valuationRequestDTO.getStaffID()).orElse(null);
        int flag = 0;
        if (valuationRequest.getStatus().equals(RequestStatus.CANCEL)) {
            if (valuationRequestDTO.getStaffID() != null) {
                throw new APIException(HttpStatus.BAD_REQUEST, "You can't assign staff when valuation request has been canceled");
            }
        }

        if(valuationRequest.getStaff() == null) {
            if (valuationRequestDTO.getStaffID() != null) {
                Notification notificationDTO = new Notification();
                notificationDTO.setAccount(staff.getAccount());
                notificationDTO.setMessage("You have been assigned to valuation request number #" + valuationRequest.getId());
                notificationDTO.setRead(false);
                notificationDTO.setCreationDate(new Date());
                notificationRepository.save(notificationDTO);
                if(flag == 0) {
                    notificationDTO = new Notification();
                    notificationDTO.setAccount(valuationRequest.getCustomer().getAccount());
                    notificationDTO.setMessage("Your valuation request number #" + valuationRequest.getId() + " has been approved");
                    notificationDTO.setRead(false);
                    notificationDTO.setCreationDate(new Date());
                    notificationRepository.save(notificationDTO);
                    flag = 1;
                }
            }
        }else{
            if (!Objects.equals(valuationRequest.getStaff().getId(), valuationRequestDTO.getStaffID())) {
                Notification notificationDTO = new Notification();
                notificationDTO.setAccount(staff.getAccount());
                notificationDTO.setMessage("You have been assigned to valuation request number #" + valuationRequest.getId());
                notificationDTO.setRead(false);
                notificationDTO.setCreationDate(new Date());
                notificationRepository.save(notificationDTO);
                if(flag == 0) {
                    notificationDTO = new Notification();
                    notificationDTO.setAccount(valuationRequest.getCustomer().getAccount());
                    notificationDTO.setMessage("Your valuation request number #" + valuationRequest.getId() + " has been approved");
                    notificationDTO.setRead(false);
                    notificationDTO.setCreationDate(new Date());
                    notificationRepository.save(notificationDTO);
                    flag = 1;
                }
            }
        }

        valuationRequest.setStaff(staff);

        //update valuation request
        valuationRequest.setFeedback(valuationRequestDTO.getFeedback());
        valuationRequest.setReturnDate(valuationRequestDTO.getReturnDate());
        valuationRequest.setStatus(valuationRequestDTO.getStatus());
        valuationRequest.setCancelReason(valuationRequestDTO.getCancelReason());

        if(valuationRequest.getStatus().toString().equals(RequestStatus.CANCEL.toString())) {
            if(valuationRequest.getCancelReason() == null ||
                    valuationRequest.getCancelReason().isEmpty()) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Please provide cancel reason!");
            }
        }
        //save to database
        valuationRequest = valuationRequestRepository.save(valuationRequest);

        if (valuationRequest.getStatus().toString().equalsIgnoreCase(SEALED.toString())) {
                Notification notificationDTO = new Notification();
                notificationDTO.setAccount(accountRepository.findByRole(Role.MANAGER));
                notificationDTO.setRead(false);
                notificationDTO.setCreationDate(new Date());
                notificationDTO.setMessage("Request number #" + valuationRequest.getId() + " has been sealed");
                notificationRepository.save(notificationDTO);
        }
        //map to dto
        valuationRequestDTO = mapToDTO(valuationRequest);
        return valuationRequestDTO;
    }

    @Override
    public ValuationRequestDTO deleteValuationRequestById(Long id) {
        ValuationRequest valuationRequest = valuationRequestRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation Request", "id", id + ""));
        if (!valuationRequest.getStatus().toString().equalsIgnoreCase(RequestStatus.PENDING.toString())) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Can't cancel this valuation request!");
        }
        valuationRequest.setStatus(RequestStatus.CANCEL);
        valuationRequest = valuationRequestRepository.save(valuationRequest);
        Notification notificationDTO = new Notification();
        notificationDTO.setAccount(accountRepository.findByRole(Role.MANAGER));
        notificationDTO.setRead(false);
        notificationDTO.setCreationDate(new Date());
        notificationDTO.setMessage("Request number #" + valuationRequest.getId() + " has been canceled");
        notificationRepository.save(notificationDTO);
        return mapToDTO(valuationRequest);
    }

    @Override
    public Response<ValuationRequestResponseV2> getValuationRequestResponse(
            int pageNo, int pageSize, String sortBy, String sortDir, RequestStatus status, Date startDate, Date endDate, String searchValue) {
        Integer requestId = null;
        String customerName = "";
        String phone = "";

        if (searchValue != null && !searchValue.trim().isEmpty()) {
            try {
                requestId = Integer.parseInt(searchValue);
            } catch (NumberFormatException e) {
            }
            customerName = searchValue;
            phone = searchValue;
        }
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<ValuationRequest> page;
        page = valuationRequestRepository.findAllByStatusAndCreationDateBetweenAndSearch(status, startDate, endDate, requestId, customerName, phone, searchValue,pageable);

        List<ValuationRequest> valuationRequests = page.getContent();

        List<ValuationRequestResponseV2> listDTO = valuationRequests.
                stream().
                map(this::mapToResponse).collect(Collectors.toList());

        Response<ValuationRequestResponseV2> response = new Response<>();

        response.setContent(listDTO);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;
    }

    @Override
    public Response<ValuationRequestResponseV2> getValuationRequestResponseByStaff(
            int pageNo, int pageSize, String sortBy, String sortDir, Long staffId, RequestStatus status, Date startDate, Date endDate,String searchValue) {
        Integer requestId = null;
        String customerName = "";
        String phone = "";

        if (searchValue != null && !searchValue.trim().isEmpty()) {
            try {
                requestId = Integer.parseInt(searchValue);
            } catch (NumberFormatException e) {
            }
            customerName = searchValue;
            phone = searchValue;
        }
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<ValuationRequest> page;

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId + ""));
        page = valuationRequestRepository.findValuationRequestByStaff_Id(staff, status, startDate, endDate, requestId, customerName, phone, searchValue, pageable);

        List<ValuationRequest> valuationRequests = page.getContent();

        List<ValuationRequestResponseV2> listDTO = valuationRequests.
                stream().
                map(valuationRequest -> this.mapToResponse(valuationRequest, ValuationRequestResponseV2.class)).collect(Collectors.toList());

        Response<ValuationRequestResponseV2> response = new Response<>();

        response.setContent(listDTO);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;
    }

    @Override
    public Response<ValuationRequestResponseV2> getValuationRequestByCustomerId(int pageNo,
                                                                                int pageSize,
                                                                                String sortBy,
                                                                                String sortDir,
                                                                                Long customerId) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<ValuationRequest> page = valuationRequestRepository.findValuationRequestByCustomer_Id(customerId, pageable);
        List<ValuationRequest> valuationRequests = page.getContent();

        List<ValuationRequestResponseV2> listDTO = valuationRequests.
                stream().
                map(valuationRequest -> this.mapToResponse(valuationRequest, ValuationRequestResponseV2.class)).collect(Collectors.toList());

        Response<ValuationRequestResponseV2> response = new Response<>();

        response.setContent(listDTO);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;
    }

    private ValuationRequestDTO mapToDTO(ValuationRequest valuationRequest) {
        ValuationRequestDTO valuationRequestDTO = mapper.map(valuationRequest, ValuationRequestDTO.class);
        Set<ValuationRequestDetailDTO> valuationRequestDetailDTOSet = new HashSet<>();
        for (ValuationRequestDetail valuationRequestDetail : valuationRequest.getValuationRequestDetails()) {
            ValuationRequestDetailDTO valuationRequestDetailDTO = mapper.map(valuationRequestDetail, ValuationRequestDetailDTO.class);
            if (valuationRequestDetail.getDiamondValuationNote() != null
                    && valuationRequestDetail.getDiamondValuationNote().getClarityCharacteristic() != null) {
                DiamondValuationNoteDTO diamondValuationNoteDTO =
                        diamondValuationNoteServiceImpl.getDiamondValuationNoteById(
                                valuationRequestDetail.getDiamondValuationNote().getId());
                valuationRequestDetailDTO.setDiamondValuationNote(diamondValuationNoteDTO);
            }
            valuationRequestDetailDTOSet.add(valuationRequestDetailDTO);
        }
        valuationRequestDTO.setValuationRequestDetails(valuationRequestDetailDTOSet);
        return valuationRequestDTO;
    }

    private ValuationRequestResponseV2 mapToResponse(ValuationRequest valuationRequest) {
        ValuationRequestResponseV2 valuationRequestResponseV2 = mapper.map(valuationRequest, ValuationRequestResponseV2.class);
        valuationRequestResponseV2.setServiceName(valuationRequest.getService().getServiceName());
        valuationRequestResponseV2.setCustomerFirstName(valuationRequest.getCustomer().getFirstName());
        valuationRequestResponseV2.setCustomerLastName(valuationRequest.getCustomer().getLastName());
        return valuationRequestResponseV2;
    }
    private <T> T mapToResponse(ValuationRequest valuationRequest, Class<T> responseType) {
        return mapper.map(valuationRequest, responseType);
    }

    private ValuationRequest mapToEntity(ValuationRequestDTO valuationRequestDTO) {
        ValuationRequest valuationRequest = mapper.map(valuationRequestDTO, ValuationRequest.class);
        long staffId = valuationRequestDTO.getStaffID();
        if (staffId == 0) {
            valuationRequest.setStaff(null);
        }
        return valuationRequest;
    }

}
