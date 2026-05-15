package com.emsafe.history.dto;

import com.emsafe.history.entity.History;

import java.time.LocalDate;

public record HistoryDto(
        Long id,
        String orderId,
        LocalDate completionDate,
        String completionTime,
        String client,
        String site,
        String serviceType,
        String technician,
        String technicianInitials,
        String status
) {
    public static HistoryDto from(History h) {
        return new HistoryDto(
                h.getId(),
                h.getOrderId(),
                h.getCompletionDate(),
                h.getCompletionTime(),
                h.getClient(),
                h.getSite(),
                h.getServiceType(),
                h.getTechnician(),
                h.getTechnicianInitials(),
                h.getStatus()
        );
    }
}
