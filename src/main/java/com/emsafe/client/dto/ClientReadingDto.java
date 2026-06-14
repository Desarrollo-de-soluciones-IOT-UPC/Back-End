package com.emsafe.client.dto;

/**
 * A single radiation reading belonging to one of the client's devices.
 */
public record ClientReadingDto(
        Long id,
        Double value,
        String level,
        String readingDate,
        Long deviceId,
        String deviceName
) {}
