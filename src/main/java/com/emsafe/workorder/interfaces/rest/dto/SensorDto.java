package com.emsafe.workorder.interfaces.rest.dto;

import com.emsafe.workorder.domain.model.Sensor;

public record SensorDto(
        Long id,
        String sensorId,
        String location,
        String status
) {
    public static SensorDto from(Sensor s) {
        return new SensorDto(s.getId(), s.getSensorId(), s.getLocation(), s.getStatus());
    }
}
