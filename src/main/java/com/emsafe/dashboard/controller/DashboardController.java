package com.emsafe.dashboard.controller;

import com.emsafe.dashboard.dto.AlertDto;
import com.emsafe.dashboard.dto.ChartDataDto;
import com.emsafe.dashboard.dto.LatestWorkOrderDto;
import com.emsafe.dashboard.dto.StatsDto;
import com.emsafe.dashboard.service.DashboardService;
import com.emsafe.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStats()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<AlertDto>>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getAlerts()));
    }

    @GetMapping("/latest-work-orders")
    public ResponseEntity<ApiResponse<List<LatestWorkOrderDto>>> getLatestWorkOrders() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getLatestWorkOrders()));
    }

    @GetMapping("/chart-data")
    public ResponseEntity<ApiResponse<ChartDataDto>> getChartData() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getChartData()));
    }
}
