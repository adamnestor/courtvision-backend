package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.service.impl.DataRefreshServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/data")
public class DataManagementController {

    private final DataRefreshServiceImpl dataRefreshService;

    public DataManagementController(DataRefreshServiceImpl dataRefreshService) {
        this.dataRefreshService = dataRefreshService;
    }

    @PostMapping("/import/{year}/{month}")
    public ResponseEntity<String> importHistoricalDataByYearMonth(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        dataRefreshService.importHistoricalDataByYearMonth(year, month);
        return ResponseEntity.ok(
                String.format("Historical data import completed for %d/%d", year, month));
    }
} 