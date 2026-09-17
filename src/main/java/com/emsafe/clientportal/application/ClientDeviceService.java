package com.emsafe.clientportal.application;

import com.emsafe.clientportal.interfaces.rest.dto.ClientDeviceDto;
import com.emsafe.clientportal.interfaces.rest.dto.ClientReadingDto;
import com.emsafe.device.application.DeviceApplicationService;
import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.repository.DeviceRepository;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Los sensores del cliente autenticado y el control de su relé.
 *
 * <p>Todas las consultas van acotadas por {@code clientId}: un cliente no puede ver ni
 * accionar un sensor que no sea suyo. Para el relé, la comprobación de propiedad se
 * delega en {@link DeviceApplicationService#orderPlugForOwner}, que es donde vive esa
 * regla y donde se publica {@code PlugOrdered}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientDeviceService {

    private final DeviceRepository deviceRepository;
    private final RadiationReadingRepository readingRepository;
    private final DeviceApplicationService devices;

    public List<ClientDeviceDto> getDevices(Long clientId) {
        return deviceRepository.findByClient(clientId).stream()
                .map(d -> ClientDeviceDto.from(d, readingRepository.findByDevice(d.getId())))
                .toList();
    }

    public ClientDeviceDto getDeviceDetail(Long clientId, Long deviceId) {
        Device d = requireOwned(clientId, deviceId);
        return ClientDeviceDto.from(d, readingRepository.findByDevice(deviceId));
    }

    public List<ClientReadingDto> getDeviceReadings(Long clientId, Long deviceId) {
        requireOwned(clientId, deviceId);   // lanza 404 si el sensor no es de este cliente
        return readingRepository.findByDevice(deviceId).stream()
                .map(ClientReadingDto::from)
                .toList();
    }

    /**
     * Guarda el estado del relé que el usuario quiere (ON | OFF) para uno de SUS
     * sensores. El edge lo recoge con {@code GET /api/v1/devices/{serial}/plug} y
     * acciona el equipo (mobile → backend → edge → dispositivo).
     */
    @Transactional
    public ClientDeviceDto setDesiredPlug(Long clientId, Long deviceId, String plug) {
        Device d = devices.orderPlugForOwner(deviceId, clientId, plug);
        return ClientDeviceDto.from(d, readingRepository.findByDevice(deviceId));
    }

    private Device requireOwned(Long clientId, Long deviceId) {
        return deviceRepository.findByIdAndClient(deviceId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", deviceId));
    }
}
