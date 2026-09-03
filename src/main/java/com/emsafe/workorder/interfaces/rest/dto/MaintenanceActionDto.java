package com.emsafe.workorder.interfaces.rest.dto;

import com.emsafe.workorder.domain.model.MaintenanceAction;

public record MaintenanceActionDto(
        Long id,
        Long deviceId,
        String deviceName,
        String action,
        String description
) {
    public static MaintenanceActionDto from(MaintenanceAction m) {
        return new MaintenanceActionDto(
                m.getId(),
                m.getDeviceId(),
                m.getDeviceName(),
                m.getAction(),
                m.getDescription()
        );
    }
}
