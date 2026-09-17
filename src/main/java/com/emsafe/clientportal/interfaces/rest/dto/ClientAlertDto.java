package com.emsafe.clientportal.interfaces.rest.dto;

import com.emsafe.monitoring.domain.model.RadiationReading;

/**
 * Alerta derivada de una lectura del cliente que superó el umbral de precaución.
 *
 * <p>Se calculan a partir de las lecturas del propio cliente porque el agregado
 * {@code Alert} del contexto Alerting es global y no tiene relación con un cliente.
 */
public record ClientAlertDto(
        Long id,
        String type,
        String level,
        String title,
        String description,
        Double value,
        Long deviceId,
        String deviceName,
        String time,
        String recordedAt   // marca ISO exacta (para el "hace 36 min")
) {
    public static ClientAlertDto from(RadiationReading r) {
        String level = r.levelForApi();
        String deviceName = r.getDevice() != null ? r.getDevice().getName() : null;
        return new ClientAlertDto(
                r.getId(),
                level,
                level,
                r.isDangerous() ? "Critical radiation level" : "Elevated radiation level",
                (deviceName != null ? deviceName : "Sensor") + " recorded " + r.getValue() + " µT",
                r.getValue(),
                r.getDevice() != null ? r.getDevice().getId() : null,
                deviceName,
                r.getReadingDate() != null ? r.getReadingDate().toString() : null,
                r.getRecordedAt() != null ? r.getRecordedAt().toString() : null
        );
    }
}
