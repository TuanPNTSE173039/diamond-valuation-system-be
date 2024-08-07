package com.letitbee.diamondvaluationsystem.service;

import com.letitbee.diamondvaluationsystem.entity.ValuationRequest;
import com.letitbee.diamondvaluationsystem.enums.RequestStatus;
import com.letitbee.diamondvaluationsystem.enums.Role;
import com.letitbee.diamondvaluationsystem.payload.Response;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestDTO;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestResponse;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestResponseV2;

import java.util.Date;

public interface ValuationRequestService {

    Response<ValuationRequestResponse> getAllValuationRequests(int pageNo, int pageSize, String sortBy, String sortDir, Date startDate, Date endDate);

    ValuationRequestDTO getValuationRequestById(Long id);

    ValuationRequestDTO createValuationRequest(ValuationRequestDTO valuationRequestDto);

    ValuationRequestDTO updateValuationRequest(long id, ValuationRequestDTO valuationRequestDTO);
    ValuationRequestDTO deleteValuationRequestById(Long id);

    Response<ValuationRequestResponseV2> getValuationRequestResponse(
            int pageNo, int pageSize, String sortBy, String sortDir, RequestStatus status, Date startDate, Date endDate,String searchValue);

    Response<ValuationRequestResponseV2> getValuationRequestResponseByStaff(
            int pageNo, int pageSize, String sortBy, String sortDir, Long staffId, RequestStatus status, Date startDate, Date endDate,String searchValue);

    Response<ValuationRequestResponseV2> getValuationRequestByCustomerId(int pageNo, int pageSize, String sortBy, String sortDir, Long customerId);

}
