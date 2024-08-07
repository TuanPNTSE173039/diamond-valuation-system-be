package com.letitbee.diamondvaluationsystem.controller;

import com.letitbee.diamondvaluationsystem.enums.RequestStatus;
import com.letitbee.diamondvaluationsystem.enums.Role;
import com.letitbee.diamondvaluationsystem.payload.Response;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestDTO;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestResponse;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestResponseV2;
import com.letitbee.diamondvaluationsystem.service.ValuationRequestService;
import com.letitbee.diamondvaluationsystem.utils.AppConstraint;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Controller
@RequestMapping("api/v1/valuation-requests")
public class ValuationRequestController {
    private ValuationRequestService valuationRequestService;

    public ValuationRequestController(ValuationRequestService valuationRequestService) {
        this.valuationRequestService = valuationRequestService;
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'CONSULTANT_STAFF', 'VALUATION_STAFF')")
    @GetMapping
    public ResponseEntity<Response<ValuationRequestResponse>>
    getAllValuationRequest(@RequestParam(name = "pageNo", defaultValue = AppConstraint.PAGE_NO, required = false) int pageNo,
                           @RequestParam(name = "pageSize", defaultValue = AppConstraint.PAGE_SIZE, required = false) int pageSize,
                           @RequestParam(name = "sortBy", defaultValue = AppConstraint.SORT_BY, required = false) String sortBy,
                           @RequestParam(name = "sortDir", defaultValue = AppConstraint.SORT_DIR, required = false) String sortDir,
                           @RequestParam(name = "startDate", defaultValue = AppConstraint.START_DATE, required = false) String startDate,
                           @RequestParam(name = "endDate", defaultValue = AppConstraint.END_DATE, required = false) String endDate
                           ) {
        Date startDateParse = new Date(startDate);
        Date endDateParse = new Date(endDate);
        return new ResponseEntity<>(valuationRequestService.getAllValuationRequests(pageNo, pageSize, sortBy, sortDir, startDateParse, endDateParse), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'CONSULTANT_STAFF', 'CUSTOMER', 'VALUATION_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ValuationRequestDTO> getValuationRequest(@PathVariable("id") long id) {
        return ResponseEntity.ok(valuationRequestService.getValuationRequestById(id));
    }

    @PostMapping
    public ResponseEntity<ValuationRequestDTO> createValuationRequest(
            @RequestBody @Valid ValuationRequestDTO valuationRequestDT) {
        return new ResponseEntity<>(valuationRequestService.createValuationRequest(valuationRequestDT), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'CUSTOMER', 'CONSULTANT_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ValuationRequestDTO> updateValuationRequest(
            @PathVariable("id") long id,
            @RequestBody @Valid ValuationRequestDTO valuationRequestDT) {
        return ResponseEntity.ok(valuationRequestService.updateValuationRequest(id, valuationRequestDT));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'CUSTOMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ValuationRequestDTO> deleteValuationRequest(
            @PathVariable("id") long id) {
        return ResponseEntity.ok(valuationRequestService.deleteValuationRequestById(id));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'CONSULTANT_STAFF')")
    @GetMapping("/response")
    public ResponseEntity<Response<ValuationRequestResponseV2>> getValuationRequestResponse(
            @RequestParam(name = "pageNo", defaultValue = AppConstraint.PAGE_NO, required = false) int pageNo,
            @RequestParam(name = "pageSize", defaultValue = AppConstraint.PAGE_SIZE, required = false) int pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstraint.SORT_BY, required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = AppConstraint.SORT_DIR, required = false) String sortDir,
            @RequestParam(name = "status", required = false) RequestStatus status,
            @RequestParam(name = "startDate", defaultValue = AppConstraint.START_DATE, required = false) String startDate,
            @RequestParam(name = "endDate", defaultValue = AppConstraint.END_DATE, required = false) String endDate,
            @RequestParam(name = "search", required = false) String searchValue){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate startDateLD = LocalDate.parse(startDate, formatter);
        LocalDate endDateLD = LocalDate.parse(endDate, formatter);

        LocalDateTime startOfDay = startDateLD.atStartOfDay();
        LocalDateTime endOfDay = endDateLD.atTime(23, 59, 59);

        Date start = java.sql.Timestamp.valueOf(startOfDay);
        Date end = java.sql.Timestamp.valueOf(endOfDay);
        return new ResponseEntity<>(valuationRequestService.
                getValuationRequestResponse(pageNo, pageSize, sortBy, sortDir,status, start, end, searchValue), HttpStatus.OK);
    }



    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Response<ValuationRequestResponseV2>> getValuationRequestByCustomerId(
            @RequestParam(name = "pageNo", defaultValue = AppConstraint.PAGE_NO, required = false) int pageNo,
            @RequestParam(name = "pageSize", defaultValue = AppConstraint.PAGE_SIZE, required = false) int pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstraint.SORT_BY, required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = AppConstraint.SORT_DIR, required = false) String sortDir,
            @PathVariable("customerId") Long customerId){
        return new ResponseEntity<>(valuationRequestService.getValuationRequestByCustomerId(pageNo, pageSize, sortBy, sortDir, customerId), HttpStatus.OK);
    }

}
