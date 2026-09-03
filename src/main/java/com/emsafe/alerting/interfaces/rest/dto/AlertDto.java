package com.emsafe.alerting.interfaces.rest.dto;

import com.emsafe.alerting.domain.model.Alert;

public record AlertDto(
        Long id,
        String type,
        String icon,
        String title,
        String description,
        String time,
        Boolean resolved,
        String resolvedAt,
        String recipientType,
        String clientName,
        String sensor
) {
    public static AlertDto from(Alert a) {
        return new AlertDto(
                a.getId(),
                a.getType().persistedValue(),
                a.getIcon(),
                a.getTitle(),
                a.getDescription(),
                a.getRelativeTime(),
                a.getResolved(),
                a.getResolvedAt() != null ? a.getResolvedAt().toString() : null,
                a.getRecipientType(),
                a.getClientName(),
                a.getSensor()
        );
    }
}
