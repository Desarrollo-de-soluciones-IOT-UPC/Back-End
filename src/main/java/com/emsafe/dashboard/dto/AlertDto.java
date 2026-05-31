package com.emsafe.dashboard.dto;

import com.emsafe.dashboard.entity.Alert;

public record AlertDto(
        Long id,
        String type,
        String icon,
        String title,
        String description,
        String time,
        Boolean resolved,
        String resolvedAt
) {
    public static AlertDto from(Alert a) {
        return new AlertDto(
                a.getId(),
                a.getType(),
                a.getIcon(),
                a.getTitle(),
                a.getDescription(),
                a.getRelativeTime(),
                a.getResolved(),
                a.getResolvedAt() != null ? a.getResolvedAt().toString() : null
        );
    }
}
