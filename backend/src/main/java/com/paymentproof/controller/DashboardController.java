package com.paymentproof.controller;

import com.paymentproof.dto.DashboardSummaryDto;
import com.paymentproof.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        log.info("Fetching authoritative dashboard summary statistics");
        DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}
