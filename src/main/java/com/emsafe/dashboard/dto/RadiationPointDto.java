package com.emsafe.dashboard.dto;

public record RadiationPointDto(
        Long id,
        Double latitude,
        Double longitude,
        String location,
        String sensorId,
        Double value,
        String readingDate
) {}
