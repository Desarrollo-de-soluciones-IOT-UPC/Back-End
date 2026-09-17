package com.emsafe.clientportal.interfaces.rest.dto;

import com.emsafe.monitoring.domain.model.RadiationReading;

/**
 * Una lectura de radiación de uno de los sensores del cliente.
 */
public record ClientReadingDto(
        Long id,
        Double value,
        String level,
        String readingDate,
        String recordedAt,   // marca ISO exacta de la medición (para mostrar la hora)
        Long deviceId,
        String deviceName
) {
    public static ClientReadingDto from(RadiationReading r) {
        return new ClientReadingDto(
                r.getId(),
                r.getValue(),
                r.levelForApi(),
                r.getReadingDate() != null ? r.getReadingDate().toString() : null,
                r.getRecordedAt() != null ? r.getRecordedAt().toString() : null,
                r.getDevice() != null ? r.getDevice().getId() : null,
                r.getDevice() != null ? r.getDevice().getName() : null
        );
    }
}
