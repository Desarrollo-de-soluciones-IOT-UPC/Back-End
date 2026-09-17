package com.emsafe.clientportal.interfaces.rest.dto;

import com.emsafe.assistant.domain.model.SensorContext;
import com.emsafe.device.domain.model.Device;
import com.emsafe.monitoring.domain.model.RadiationReading;

import java.util.Comparator;
import java.util.List;

/**
 * Un sensor del cliente con la instantánea de su última lectura.
 */
public record ClientDeviceDto(
        Long id,
        String name,
        String type,
        String location,
        String status,
        String serialNumber,
        String installDate,
        Double latestValue,
        String latestLevel,
        String latestReadingDate,
        int readingsCount,
        String plug,          // estado del relé reportado por el equipo (ON | OFF | null)
        String desiredPlug    // estado del relé ordenado por el usuario (ON | OFF | null)
) {

    public static ClientDeviceDto from(Device d, List<RadiationReading> deviceReadings) {
        // Se prefiere la marca exacta (recordedAt): con el edge enviando varias lecturas
        // al día, readingDate por sí sola no distingue cuál es la última.
        RadiationReading latest = deviceReadings.stream()
                .filter(r -> r.getRecordedAt() != null || r.getReadingDate() != null)
                .max(Comparator.comparing(RadiationReading::timestamp))
                .orElse(null);

        return new ClientDeviceDto(
                d.getId(),
                d.getName(),
                d.getType(),
                d.getLocation(),
                d.getStatus().persistedValue(),
                d.getSerialNumber(),
                d.getInstallDate() != null ? d.getInstallDate().toString() : null,
                latest != null ? latest.getValue() : null,
                latest != null ? latest.levelForApi() : null,
                latest != null && latest.getReadingDate() != null
                        ? latest.getReadingDate().toString() : null,
                deviceReadings.size(),
                latest != null ? latest.getPlug() : null,
                d.getDesiredPlug() != null ? d.getDesiredPlug().persistedValue() : null
        );
    }

    /**
     * Traducción al vocabulario del asistente (capa anticorrupción de salida): Astra
     * solo necesita saber nombre, zona, medida, nivel y relé — nada de ids ni seriales.
     */
    public SensorContext toSensorContext() {
        return new SensorContext(name, location, latestValue, latestLevel, plug);
    }
}
