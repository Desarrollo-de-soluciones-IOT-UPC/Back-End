package com.emsafe.telemetry.service;

import com.emsafe.dashboard.entity.RadiationReading;
import com.emsafe.dashboard.repository.RadiationReadingRepository;
import com.emsafe.device.entity.Device;
import com.emsafe.device.repository.DeviceRepository;
import com.emsafe.shared.exception.BadRequestException;
import com.emsafe.shared.exception.ResourceNotFoundException;
import com.emsafe.telemetry.dto.ReadingDto;
import com.emsafe.telemetry.dto.ReadingIngestRequest;
import com.emsafe.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ingesta de telemetría desde el edge IoT y consulta de lecturas para web/móvil.
 *
 * Enlace lectura → device → cliente: el edge manda `deviceId` (= serial del
 * sensor). Si el serial ya está registrado (con su cliente), la lectura queda
 * ligada automáticamente. Si no existe, se autocrea un device "unregistered"
 * (sin cliente) para no perder datos; luego se le asignará cliente desde el
 * portal (flujo de "reclamar sensor", pendiente).
 */
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private static final int MAX_LIMIT = 500;

    private final RadiationReadingRepository readingRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public ReadingDto ingest(ReadingIngestRequest req) {
        if (req == null || req.getSerialNumber() == null || req.getSerialNumber().isBlank()) {
            throw new BadRequestException("serialNumber is required");
        }
        if (req.getFieldUT() == null) {
            throw new BadRequestException("field_uT is required");
        }

        String serial = req.getSerialNumber().trim();
        Device device = deviceRepository.findBySerialNumber(serial);
        if (device == null) {
            // Sensor aún no registrado: lo autocreamos como "unregistered" (sin cliente).
            device = deviceRepository.save(Device.builder()
                    .name("Unregistered sensor " + serial)
                    .type("Sensor")
                    .status("unregistered")
                    .serialNumber(serial)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        LocalDateTime now = LocalDateTime.now();
        AppUser client = device.getClient();

        RadiationReading reading = RadiationReading.builder()
                .readingDate(LocalDate.now())
                .recordedAt(now)
                .value(req.getFieldUT())
                .level(req.getLevel())
                .message(req.getMessage())
                .sensorId(serial)
                .location(device.getLocation())
                .latitude(client != null ? client.getLatitude() : null)
                .longitude(client != null ? client.getLongitude() : null)
                .device(device)
                .build();

        return toDto(readingRepository.save(reading));
    }

    @Transactional(readOnly = true)
    public List<ReadingDto> list(String serialNumber, int limit) {
        Pageable pageable = PageRequest.of(0, clampLimit(limit));
        List<RadiationReading> readings = (serialNumber != null && !serialNumber.isBlank())
                ? readingRepository.findBySensorId(serialNumber.trim(), pageable)
                : readingRepository.findRecent(pageable);
        return readings.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ReadingDto latest(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new BadRequestException("serialNumber is required");
        }
        return readingRepository.findBySensorId(serialNumber.trim(), PageRequest.of(0, 1))
                .stream().findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No readings found for serialNumber: " + serialNumber));
    }

    private int clampLimit(int limit) {
        if (limit <= 0) return 50;
        return Math.min(limit, MAX_LIMIT);
    }

    private ReadingDto toDto(RadiationReading r) {
        Device d = r.getDevice();
        AppUser client = d != null ? d.getClient() : null;
        return ReadingDto.builder()
                .id(r.getId())
                .serialNumber(r.getSensorId())
                .deviceDbId(d != null ? d.getId() : null)
                .deviceName(d != null ? d.getName() : null)
                .fieldUT(r.getValue())
                .level(r.getLevel())
                .message(r.getMessage())
                .location(r.getLocation())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .clientId(client != null ? client.getId() : null)
                .clientName(client != null ? client.getName() : null)
                .recordedAt(r.getRecordedAt())
                .build();
    }
}
