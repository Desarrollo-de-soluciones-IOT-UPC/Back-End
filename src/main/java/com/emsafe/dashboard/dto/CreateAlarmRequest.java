package com.emsafe.dashboard.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAlarmRequest(
        @NotBlank String type,
        String icon,
        @NotBlank String title,
        String description,
        String relativeTime
) {}
