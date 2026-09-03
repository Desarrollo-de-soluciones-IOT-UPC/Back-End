package com.emsafe.monitoring.interfaces.rest;

import com.emsafe.shared.interfaces.rest.ApiResponse;
import com.emsafe.monitoring.interfaces.rest.dto.ReadingDto;
import com.emsafe.monitoring.interfaces.rest.dto.ReadingIngestRequest;
import com.emsafe.monitoring.application.TelemetryApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de telemetría IoT (v1).
 *
 * - POST /api/v1/readings                       → ingesta desde el edge.
 * - GET  /api/v1/readings                       → lecturas recientes (filtro opcional ?serialNumber=, ?limit=).
 * - GET  /api/v1/readings/{serialNumber}/latest → última lectura de un sensor.
 *
 * `/api/v1/**` está expuesto en SecurityConfig (el edge no tiene JWT).
 */
@RestController
@RequestMapping("/api/v1/readings")
@RequiredArgsConstructor
public class ReadingController {

    private final TelemetryApplicationService telemetryApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReadingDto>> ingest(@RequestBody ReadingIngestRequest request) {
        ReadingDto saved = telemetryApplicationService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reading stored", saved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReadingDto>>> list(
            @RequestParam(required = false) String serialNumber,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(telemetryApplicationService.list(serialNumber, limit)));
    }

    @GetMapping("/{serialNumber}/latest")
    public ResponseEntity<ApiResponse<ReadingDto>> latest(@PathVariable String serialNumber) {
        return ResponseEntity.ok(ApiResponse.ok(telemetryApplicationService.latest(serialNumber)));
    }
}
