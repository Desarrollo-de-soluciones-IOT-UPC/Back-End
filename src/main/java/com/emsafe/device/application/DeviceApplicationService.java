package com.emsafe.device.application;

import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.device.domain.event.DeviceClaimed;
import com.emsafe.device.domain.event.DeviceReleased;
import com.emsafe.device.domain.event.PlugOrdered;
import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.model.DeviceStatus;
import com.emsafe.device.domain.model.PlugState;
import com.emsafe.device.domain.repository.DeviceRepository;
import com.emsafe.device.interfaces.rest.dto.CreateDeviceRequest;
import com.emsafe.device.interfaces.rest.dto.DeviceDto;
import com.emsafe.device.interfaces.rest.dto.DiscoverableDeviceDto;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.shared.domain.event.DomainEventPublisher;
import com.emsafe.shared.domain.exception.BadRequestException;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso del contexto Device. Orquesta; las reglas están en el agregado.
 */
@Service
@RequiredArgsConstructor
public class DeviceApplicationService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final RadiationReadingRepository readingRepository;
    private final DomainEventPublisher events;

    // ─── Consultas ────────────────────────────────────────────────────────────

    public List<DeviceDto> getAll() {
        return deviceRepository.findAllWithClient().stream().map(DeviceDto::from).toList();
    }

    /**
     * Sensores descubiertos por el edge y aún sin cliente, enriquecidos con su última
     * lectura para que el técnico pueda identificarlos físicamente durante una
     * instalación (prueba de agitado).
     */
    @Transactional(readOnly = true)
    public List<DiscoverableDeviceDto> getDiscoverable() {
        return deviceRepository.findByStatus(DeviceStatus.UNREGISTERED).stream().map(d -> {
            RadiationReading last = readingRepository
                    .findLatestBySensorId(d.getSerialNumber())
                    .orElse(null);
            return new DiscoverableDeviceDto(
                    d.getId(),
                    d.getSerialNumber(),
                    d.getName(),
                    d.getType(),
                    last != null ? last.getValue() : null,
                    last != null ? last.getLevel() : null,
                    last != null ? last.getRecordedAt() : null,
                    d.getSerialNumber() != null ? readingRepository.countBySensorId(d.getSerialNumber()) : 0
            );
        }).toList();
    }

    public List<DeviceDto> getByClient(Long clientId) {
        return deviceRepository.findByClient(clientId).stream().map(DeviceDto::from).toList();
    }

    public DeviceDto getById(Long id) {
        return deviceRepository.findByIdWithClient(id)
                .map(DeviceDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));
    }

    /** Estado deseado del relé — lo consulta el EDGE en cada ciclo. */
    @Transactional(readOnly = true)
    public PlugState getDesiredPlug(String serialNumber) {
        return requireBySerial(serialNumber).getDesiredPlug();
    }

    // ─── Comandos ─────────────────────────────────────────────────────────────

    @Transactional
    public DeviceDto create(CreateDeviceRequest req) {
        Device device = Device.register(
                req.name(),
                req.type(),
                req.location(),
                StringUtils.hasText(req.status()) ? DeviceStatus.fromApi(req.status()) : DeviceStatus.ACTIVE,
                req.serialNumber(),
                parseDate(req.installDate()),
                resolveClient(req.clientId())
        );
        return DeviceDto.from(deviceRepository.save(device));
    }

    @Transactional
    public DeviceDto update(Long id, CreateDeviceRequest req) {
        Device device = deviceRepository.findByIdWithClient(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));

        device.updateDetails(req.name(), req.type(), req.location(),
                req.serialNumber(), parseDate(req.installDate()));

        if (StringUtils.hasText(req.status())) {
            device.changeStatus(DeviceStatus.fromApi(req.status()));
        }
        // clientId = 0 significa "desasignar"; cualquier otro valor reasigna.
        if (req.clientId() != null) {
            device.assignTo(req.clientId() == 0 ? null : resolveClient(req.clientId()));
        }
        return DeviceDto.from(deviceRepository.save(device));
    }

    /**
     * Instalación desde una orden de trabajo: el técnico reclama un sensor del pool.
     *
     * <p>Busca por id, luego por serial, y como red de seguridad lo crea si el sensor
     * todavía no había reportado nada al edge. Esta operación es la frontera con el
     * contexto WorkOrder: allí ya no se toca el repositorio de Device.
     */
    @Transactional
    public void claimForInstallation(Long deviceId, String serialNumber, String name,
                                     String type, User client, String siteLocation) {
        Device device = null;
        if (deviceId != null) {
            device = deviceRepository.findById(deviceId).orElse(null);
        }
        if (device == null && StringUtils.hasText(serialNumber)) {
            device = deviceRepository.findBySerialNumber(serialNumber.trim()).orElse(null);
        }
        if (device == null) {
            if (!StringUtils.hasText(serialNumber)) {
                throw new BadRequestException("deviceId or serialNumber is required to claim a device");
            }
            device = Device.discoveredByEdge(serialNumber.trim());
        }

        device.claimFor(client, siteLocation, name, type);
        Device saved = deviceRepository.save(device);
        events.publish(new DeviceClaimed(saved.getId(), saved.getSerialNumber(),
                client != null ? client.getId() : null));
    }

    /** Alta directa de un sensor instalado (flujo legacy: el técnico teclea el serial). */
    @Transactional
    public void registerInstalled(String serialNumber, String name, String type,
                                  String siteLocation, User client) {
        if (!StringUtils.hasText(serialNumber)) {
            throw new BadRequestException("Serial number is required for every installed device");
        }
        Device device = Device.register(
                StringUtils.hasText(name) ? name : "Sensor",
                StringUtils.hasText(type) ? type : "Sensor",
                siteLocation,
                DeviceStatus.ACTIVE,
                serialNumber,
                LocalDate.now(),
                client);
        deviceRepository.save(device);
    }

    /**
     * Actualiza el estado de un sensor desde el parte del técnico. Pasar a
     * UNREGISTERED lo devuelve al pool (el agregado limpia cliente y fecha).
     */
    @Transactional
    public void updateStatusFromField(Long deviceId, String status) {
        if (deviceId == null || !StringUtils.hasText(status)) {
            return;
        }
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.changeStatus(DeviceStatus.fromApi(status));
            deviceRepository.save(device);
        });
    }

    /** Recolección: el sensor vuelve al pool y queda libre. */
    @Transactional
    public void release(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", deviceId));
        Long previousClientId = device.getClient() != null ? device.getClient().getId() : null;
        device.releaseToPool();
        deviceRepository.save(device);
        events.publish(new DeviceReleased(deviceId, device.getSerialNumber(), previousClientId));
    }

    /**
     * Arranca el mantenimiento de todos los sensores de un cliente que estaban
     * marcados como averiados. Lo dispara el técnico al iniciar la orden.
     */
    @Transactional
    public void startMaintenanceForClient(Long clientId) {
        deviceRepository.findByClientAndStatus(clientId, DeviceStatus.REQUIRES_MAINTENANCE)
                .forEach(d -> {
                    d.startMaintenance();
                    deviceRepository.save(d);
                });
    }

    /** Orden del usuario para el relé (mobile → backend → edge). */
    @Transactional
    public void orderPlug(Device device, PlugState desired) {
        device.orderPlug(desired);
        Device saved = deviceRepository.save(device);
        events.publish(new PlugOrdered(saved.getId(), saved.getSerialNumber(), desired));
    }

    @Transactional
    public void delete(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Device", id);
        }
        deviceRepository.deleteById(id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Device requireBySerial(String serialNumber) {
        if (!StringUtils.hasText(serialNumber)) {
            throw new com.emsafe.shared.domain.exception.BadRequestException("serialNumber is required");
        }
        return deviceRepository.findBySerialNumber(serialNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Device", 0L));
    }

    private User resolveClient(Long clientId) {
        if (clientId == null || clientId == 0) return null;
        return userRepository.findById(clientId).orElse(null);
    }

    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
