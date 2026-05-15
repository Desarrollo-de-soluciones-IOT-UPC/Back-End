package com.emsafe.workorder.dto;

import java.util.List;

/**
 * Used by technician to update status, add sensors, notes, and activity log.
 */
public record PatchWorkOrderRequest(
        String status,
        String technicianNotes,
        List<SensorPatchDto> sensors,
        ActivityLogPatchDto activityLogEntry
) {
    public record SensorPatchDto(
            String sensorId,
            String location,
            String status
    ) {}

    public record ActivityLogPatchDto(
            String event,
            String time
    ) {}
}
