package com.letitbee.diamondvaluationsystem.service;

import com.letitbee.diamondvaluationsystem.entity.DiamondValuationAssign;
import com.letitbee.diamondvaluationsystem.payload.DiamondValuationAssignDTO;
import com.letitbee.diamondvaluationsystem.payload.DiamondValuationAssignResponse;
import com.letitbee.diamondvaluationsystem.payload.Response;

public interface DiamondValuationAssignService {
    DiamondValuationAssignDTO createDiamondValuationAssign(DiamondValuationAssignDTO diamondValuationAssignDTO);
    DiamondValuationAssignDTO updateDiamondValuationAssign(long id, DiamondValuationAssignDTO diamondValuationAssignDTO);
    Response<DiamondValuationAssignResponse> getAllDiamondValuationAssign(int pageNo, int pageSize, String sortBy, String sortDir, String status);
    DiamondValuationAssignDTO getDiamondValuationAssignById(long id);
}
