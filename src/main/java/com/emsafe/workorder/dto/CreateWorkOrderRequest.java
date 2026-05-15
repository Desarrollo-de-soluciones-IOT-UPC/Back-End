package com.emsafe.workorder.dto;

import com.emsafe.workorder.entity.WorkOrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateWorkOrderRequest(
        @NotNull(message = "Type is required")
        WorkOrderType type,

        @NotBlank(message = "Client is required")
        String client,

        String location,
        String city,
        LocalDate scheduledDate,
        String scheduledTime,

        Long technicianId,

        String priority,
        String contactName,
        String contactRole,
        String contactPhone,
        String contactEmail,
        String accessInstructions,
        Integer expectedSensors,
        String assetId,
        List<String> requiredTools
) {}
