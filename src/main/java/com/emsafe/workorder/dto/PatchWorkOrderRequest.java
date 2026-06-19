package com.emsafe.workorder.dto;

import java.util.List;

/**
 * Used by the technician to drive the work-order lifecycle: status transitions,
 * notes, work-order sensors, activity log, and — depending on the service type —
 * device creation (Installation), device status updates (Maintenance/Collection),
 * maintenance actions and evidence images.
 */
public record PatchWorkOrderRequest(
        String status,
        String technicianNotes,
        String cancellationReason,
        List<SensorPatchDto> sensors,
        ActivityLogPatchDto activityLogEntry,

        // ── Installation: devices to create for the client ──
        List<NewDeviceDto> newDevices,

        // ── Maintenance / Collection: device status changes ──
        List<DeviceStatusDto> deviceUpdates,

        // ── Maintenance: actions performed ──
        List<MaintenanceActionPatchDto> maintenanceActions,

        // ── Evidence images (base64 data-URLs) ──
        List<String> evidence
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

    public record NewDeviceDto(
            String name,
            String type,
            String serialNumber
    ) {}

    public record DeviceStatusDto(
            Long deviceId,
            String status,
            String observation
    ) {}

    public record MaintenanceActionPatchDto(
            Long deviceId,
            String deviceName,
            String action,
            String description
    ) {}
}
