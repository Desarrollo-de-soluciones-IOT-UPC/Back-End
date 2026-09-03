package com.emsafe.monitoring.application;

import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.repository.DeviceRepository;
import com.emsafe.iam.domain.model.User;
import com.emsafe.monitoring.domain.event.DangerLevelDetected;
import com.emsafe.monitoring.domain.event.ReadingIngested;
import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.port.ReadingBroadcaster;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.monitoring.interfaces.rest.dto.ReadingDto;
import com.emsafe.monitoring.interfaces.rest.dto.ReadingIngestRequest;
import com.emsafe.shared.domain.event.DomainEventPublisher;
import com.emsafe.shared.domain.exception.BadRequestException;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Casos de uso de telemetría: ingesta desde el edge y consulta para web/móvil.
 *
 * <p>Comparado con el antiguo {@code TelemetryService}, aquí ya no hay
 * {@code SimpMessagingTemplate} ni nombres de topics ni construcción manual de la
 * entidad: la lectura la crea la factoría del agregado y la difusión pasa por el
 * puerto {@link ReadingBroadcaster}.
 */
@Service
@RequiredArgsConstructor
public class TelemetryApplicationService {

    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 50;

    private final RadiationReadingRepository readingRepository;
    private final DeviceRepository deviceRepository;
    private final ReadingBroadcaster broadcaster;
    private final DomainEventPublisher events;

    @Transactional
    public ReadingDto ingest(ReadingIngestRequest req) {
        if (req == null || req.getSerialNumber() == null || req.getSerialNumber().isBlank()) {
            throw new BadRequestException("serialNumber is required");
        }

        String serial = req.getSerialNumber().trim();
        // Sensor aún no registrado: se autocrea en el pool para no perder telemetría.
        Device device = deviceRepository.findBySerialNumber(serial)
                .orElseGet(() -> deviceRepository.save(Device.discoveredByEdge(serial)));

        RadiationReading reading = RadiationReading.fromEdge(
                serial, req.getFieldUT(), req.getLevel(), req.getMessage(),
                req.getPlug(), device, LocalDateTime.now());

        RadiationReading saved = readingRepository.save(reading);
        ReadingDto dto = ReadingDto.from(saved);

        User owner = saved.owner();
        Long clientId = owner != null ? owner.getId() : null;

        broadcaster.broadcast(dto, clientId, device.isUnregistered());

        events.publish(new ReadingIngested(saved.getId(), serial, saved.getValue(),
                saved.getLevel(), clientId));
        if (saved.isDangerous()) {
            events.publish(new DangerLevelDetected(saved.getId(), serial, saved.getValue(),
                    clientId, owner != null ? owner.getName() : null));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReadingDto> list(String serialNumber, int limit) {
        int capped = clampLimit(limit);
        List<RadiationReading> readings = (serialNumber != null && !serialNumber.isBlank())
                ? readingRepository.findBySensorId(serialNumber.trim(), capped)
                : readingRepository.findRecent(capped);
        return readings.stream().map(ReadingDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ReadingDto latest(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new BadRequestException("serialNumber is required");
        }
        return readingRepository.findLatestBySensorId(serialNumber.trim())
                .map(ReadingDto::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No readings found for serialNumber: " + serialNumber));
    }

    private int clampLimit(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}
