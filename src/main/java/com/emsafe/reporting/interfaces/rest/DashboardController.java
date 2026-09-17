package com.emsafe.reporting.interfaces.rest;

import com.emsafe.alerting.interfaces.rest.dto.AlertDto;
import com.emsafe.reporting.application.DashboardQueryService;
import com.emsafe.reporting.application.RadiationMapQueryService;
import com.emsafe.reporting.interfaces.rest.dto.ChartDataDto;
import com.emsafe.reporting.interfaces.rest.dto.ClientRadiationDto;
import com.emsafe.reporting.interfaces.rest.dto.LatestWorkOrderDto;
import com.emsafe.reporting.interfaces.rest.dto.RadiationPointDto;
import com.emsafe.reporting.interfaces.rest.dto.StatsDto;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Panel y mapa del portal de administración. Rutas intactas (regla R1): el paquete
 * pasó de {@code dashboard} a {@code reporting}, la API no cambió.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboard;
    private final RadiationMapQueryService radiationMap;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboard.getStats()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<AlertDto>>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(dashboard.getAlerts()));
    }

    @GetMapping("/latest-work-orders")
    public ResponseEntity<ApiResponse<List<LatestWorkOrderDto>>> getLatestWorkOrders() {
        return ResponseEntity.ok(ApiResponse.ok(dashboard.getLatestWorkOrders()));
    }

    @GetMapping("/chart-data")
    public ResponseEntity<ApiResponse<ChartDataDto>> getChartData() {
        return ResponseEntity.ok(ApiResponse.ok(dashboard.getChartData()));
    }

    @GetMapping("/radiation-map")
    public ResponseEntity<ApiResponse<List<RadiationPointDto>>> getRadiationMap() {
        return ResponseEntity.ok(ApiResponse.ok(radiationMap.getRadiationMap()));
    }

    @GetMapping("/radiation-map/by-client")
    public ResponseEntity<ApiResponse<List<ClientRadiationDto>>> getRadiationMapByClient() {
        return ResponseEntity.ok(ApiResponse.ok(radiationMap.getRadiationMapByClient()));
    }
}
