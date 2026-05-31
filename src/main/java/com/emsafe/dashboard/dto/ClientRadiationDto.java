package com.emsafe.dashboard.dto;

import java.util.List;

public record ClientRadiationDto(
        Long clientId,
        String clientName,
        Double latitude,
        Double longitude,
        String location,
        Double maxValue,
        String level,           // "safe" | "caution" | "danger"
        List<DeviceReadingDto> devices
) {
    public record DeviceReadingDto(
            Long deviceId,
            String deviceName,
            String deviceType,
            String serialNumber,
            String deviceLocation,
            String deviceStatus,
            Double latestValue,
            String readingDate
    ) {}
}
