package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.clientportal.application.ClientMonitoringService;
import com.emsafe.clientportal.interfaces.rest.dto.ClientAlertDto;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Alertas del cliente, derivadas de sus propias lecturas. */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientAlertController {

    private final ClientMonitoringService monitoring;
    private final AuthenticatedClient client;

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<ClientAlertDto>>> getAlerts(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(monitoring.getAlerts(client.id(request))));
    }
}
