package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.reporting.application.ClientReportService;
import com.emsafe.reporting.interfaces.rest.dto.ClientReportDto;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Informe agregado de radiación — ?period=month (últimos 30 días) | year (12 meses).
 *
 * <p>El controller es del portal móvil, pero el cálculo vive en Reporting: el BFF solo
 * pone la ruta y el dueño de la petición.
 */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientReportController {

    private final ClientReportService reports;
    private final AuthenticatedClient client;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<ClientReportDto>> getReport(
            HttpServletRequest request,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(ApiResponse.ok(
                reports.forClient(client.id(request), period)));
    }
}
