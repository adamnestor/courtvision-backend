package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.service.PickResultService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/picks/results")
public class PickResultController {
    private final PickResultService pickResultService;

    public PickResultController(PickResultService pickResultService) {
        this.pickResultService = pickResultService;
    }

    //@PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/process/{date}")
    public ResponseEntity<ServiceResponse<Void>> processResultsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            pickResultService.processResultsForDate(date);
            return ResponseEntity.ok(ServiceResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error("Failed to process results: " + e.getMessage()));
        }
    }
}