package com.emsafe.device.interfaces.rest;

import com.emsafe.device.interfaces.rest.dto.DiscoverableDeviceDto;
import com.emsafe.device.application.DeviceApplicationService;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Technician device endpoints (TECHNICIAN + ADMIN via {@code /api/tech/**}).
 */
@RestController
@RequestMapping("/api/tech/devices")
@RequiredArgsConstructor
public class TechDeviceController {

    private final DeviceApplicationService deviceApplicationService;

    /** Sensors discovered by the edge, not yet assigned, available to claim during an installation. */
    @GetMapping("/discoverable")
    public ResponseEntity<ApiResponse<List<DiscoverableDeviceDto>>> getDiscoverable() {
        return ResponseEntity.ok(ApiResponse.ok(deviceApplicationService.getDiscoverable()));
    }
}
