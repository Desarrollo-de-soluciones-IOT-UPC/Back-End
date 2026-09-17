package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.clientportal.application.ClientMonitoringService;
import com.emsafe.clientportal.interfaces.rest.dto.ClientDashboardDto;
import com.emsafe.clientportal.interfaces.rest.dto.ClientReadingDto;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lecturas y panel del cliente autenticado. */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientMonitoringController {

    private final ClientMonitoringService monitoring;
    private final AuthenticatedClient client;

    @GetMapping("/readings")
    public ResponseEntity<ApiResponse<List<ClientReadingDto>>> getReadings(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(monitoring.getReadings(client.id(request))));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ClientDashboardDto>> getDashboard(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(monitoring.getDashboard(client.id(request))));
    }
}
