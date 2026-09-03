package com.emsafe.device.interfaces.rest;

import com.emsafe.device.application.DeviceApplicationService;
import com.emsafe.device.domain.model.PlugState;
import com.emsafe.device.interfaces.rest.dto.PlugStateDto;
import com.emsafe.shared.interfaces.rest.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de estado del relé (plug) para el EDGE (v1, sin JWT — igual que /api/v1/readings).
 *
 * <p>GET /api/v1/devices/{serialNumber}/plug → estado DESEADO del relé (lo que el
 * usuario ordenó desde la app). El edge lo consulta en cada ciclo y se lo pasa al
 * dispositivo para abrir o cortar la corriente.
 *
 * <p>Este controller vivía en el contexto de telemetría; con la migración a DDD pasa
 * a Device, que es el agregado dueño del relé. La telemetría solo <i>reporta</i> el
 * estado real en cada lectura; la <i>orden</i> pertenece al dispositivo.
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DevicePlugController {

    private final DeviceApplicationService deviceApplicationService;

    @GetMapping("/{serialNumber}/plug")
    public ResponseEntity<ApiResponse<PlugStateDto>> desiredPlug(@PathVariable String serialNumber) {
        PlugState desired = deviceApplicationService.getDesiredPlug(serialNumber);
        return ResponseEntity.ok(ApiResponse.ok(
                new PlugStateDto(desired != null ? desired.persistedValue() : null)));
    }
}
