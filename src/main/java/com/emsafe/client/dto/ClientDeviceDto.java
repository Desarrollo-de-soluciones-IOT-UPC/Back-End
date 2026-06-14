package com.emsafe.client.dto;

/**
 * A sensor/device owned by the client, with a snapshot of its latest reading.
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
        int readingsCount
) {}
