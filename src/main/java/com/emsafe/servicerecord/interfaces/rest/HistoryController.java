package com.emsafe.servicerecord.interfaces.rest;

import com.emsafe.iam.infrastructure.security.JwtUtil;
import com.emsafe.servicerecord.application.ServiceRecordQueryService;
import com.emsafe.servicerecord.interfaces.rest.dto.ServiceRecordDto;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import com.emsafe.shared.interfaces.rest.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Historial de servicio. Conserva las rutas {@code /api/history} y
 * {@code /api/tech/history} de siempre: el contexto se llama ServiceRecord por dentro,
 * pero el contrato REST no se toca (regla R1).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HistoryController {

    private final ServiceRecordQueryService serviceRecords;
    private final JwtUtil jwtUtil;

    /** Historial del admin — sin filtro de técnico. */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ServiceRecordDto>>> getHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(serviceRecords.find(null, status, search)));
    }

    /** Historial del técnico — filtrado por el técnico del JWT. */
    @GetMapping("/tech/history")
    public ResponseEntity<ApiResponse<List<ServiceRecordDto>>> getTechHistory(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                serviceRecords.find(extractUserId(request), status, search)));
    }

    @GetMapping("/history/paged")
    public ResponseEntity<ApiResponse<PageResponse<ServiceRecordDto>>> getHistoryPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                serviceRecords.findPaged(null, status, search, page, size)));
    }

    @GetMapping("/tech/history/paged")
    public ResponseEntity<ApiResponse<PageResponse<ServiceRecordDto>>> getTechHistoryPaged(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                serviceRecords.findPaged(extractUserId(request), status, search, page, size)));
    }

    @GetMapping("/history/export/csv")
    public void exportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"history.csv\"");
        List<ServiceRecordDto> records = serviceRecords.find(null, status, search);
        PrintWriter writer = response.getWriter();
        writer.println("Order ID,Completion Date,Completion Time,Client,Site,Service Type,Technician,Status");
        for (ServiceRecordDto r : records) {
            writer.printf("%s,%s,%s,\"%s\",\"%s\",%s,%s,%s%n",
                    r.orderId(), r.completionDate(), r.completionTime(),
                    r.client(), r.site(), r.serviceType(), r.technician(), r.status());
        }
        writer.flush();
    }

    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return jwtUtil.extractUserId(header.substring(7));
        }
        return null;
    }
}
