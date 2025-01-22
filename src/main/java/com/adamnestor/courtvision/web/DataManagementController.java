package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.service.impl.DataRefreshServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DataManagementController {

    private final DataRefreshServiceImpl dataRefreshService;

    public DataManagementController(DataRefreshServiceImpl dataRefreshService) {
        this.dataRefreshService = dataRefreshService;
    }

    @PostMapping("/import/{season}")
    public ResponseEntity<String> importHistoricalData(@PathVariable Integer season) {
        dataRefreshService.importHistoricalData(season);
        return ResponseEntity.ok("Historical data import completed for season " + season);
    }
} 