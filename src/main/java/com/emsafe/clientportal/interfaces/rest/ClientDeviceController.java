package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.clientportal.application.ClientDeviceService;
import com.emsafe.clientportal.interfaces.rest.dto.ClientDeviceDto;
import com.emsafe.clientportal.interfaces.rest.dto.ClientReadingDto;
import com.emsafe.clientportal.interfaces.rest.dto.SetPlugRequest;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sensores del cliente autenticado y control de su relé. */
@RestController
@RequestMapping("/api/client/devices")
@RequiredArgsConstructor
public class ClientDeviceController {

    private final ClientDeviceService devices;
    private final AuthenticatedClient client;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientDeviceDto>>> getDevices(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(devices.getDevices(client.id(request))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDeviceDto>> getDevice(
            HttpServletRequest request, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(devices.getDeviceDetail(client.id(request), id)));
    }

    @GetMapping("/{id}/readings")
    public ResponseEntity<ApiResponse<List<ClientReadingDto>>> getDeviceReadings(
            HttpServletRequest request, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(devices.getDeviceReadings(client.id(request), id)));
    }

    /** El cliente ordena abrir/cerrar el relé de SU dispositivo (camino de vuelta al edge). */
    @PatchMapping("/{id}/plug")
    public ResponseEntity<ApiResponse<ClientDeviceDto>> setPlug(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody SetPlugRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Plug state updated",
                devices.setDesiredPlug(client.id(request), id, req.plug())));
    }
}
