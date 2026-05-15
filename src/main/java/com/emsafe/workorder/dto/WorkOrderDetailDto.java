package com.emsafe.workorder.dto;

import com.emsafe.workorder.entity.WorkOrder;

import java.time.LocalDate;
import java.util.List;

/**
 * Full detail DTO used in technician portal.
 */
public record WorkOrderDetailDto(
        Long id,
        String orderId,
        String type,
        String status,
        String client,
        String location,
        LocalDate scheduledDate,
        String scheduledTime,
        Long technicianId,
        String priority,
        String serviceType,
        String contactName,
        String contactRole,
        String contactPhone,
        String contactEmail,
        String accessInstructions,
        Integer expectedSensors,
        String assetId,
        String technicianNotes,
        List<String> requiredTools,
        List<SensorDto> sensors,
        List<ActivityLogDto> activityLog
) {
    public static WorkOrderDetailDto from(WorkOrder wo) {
        return new WorkOrderDetailDto(
                wo.getId(),
                wo.getOrderId(),
                wo.getType().name().charAt(0) + wo.getType().name().substring(1).toLowerCase(),
                wo.getStatus().name().toLowerCase().replace("_", "-"),
                wo.getClient(),
                wo.getLocation(),
                wo.getScheduledDate(),
                wo.getScheduledTime(),
                wo.getTechnician() != null ? wo.getTechnician().getId() : null,
                wo.getPriority(),
                wo.getType().name().charAt(0) + wo.getType().name().substring(1).toLowerCase(),
                wo.getContactName(),
                wo.getContactRole(),
                wo.getContactPhone(),
                wo.getContactEmail(),
                wo.getAccessInstructions(),
                wo.getExpectedSensors(),
                wo.getAssetId(),
                wo.getTechnicianNotes(),
                wo.getRequiredTools(),
                wo.getSensors().stream().map(SensorDto::from).toList(),
                wo.getActivityLog().stream().map(ActivityLogDto::from).toList()
        );
    }
}
