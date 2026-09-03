package com.emsafe.alerting.interfaces.rest;

import com.emsafe.alerting.interfaces.rest.dto.AlertDto;
import com.emsafe.alerting.interfaces.rest.dto.CreateAlarmRequest;
import com.emsafe.alerting.application.AlarmApplicationService;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmApplicationService alarmApplicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(alarmApplicationService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertDto>> create(@Valid @RequestBody CreateAlarmRequest req) {
        AlertDto created = alarmApplicationService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Alarm created", created));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlertDto>> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alarmApplicationService.resolve(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        alarmApplicationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Alarm deleted", null));
    }
}
