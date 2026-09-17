package com.emsafe.servicerecord.interfaces.rest.dto;

import com.emsafe.servicerecord.domain.model.ServiceRecord;

import java.time.LocalDate;

/**
 * Acta de servicio tal como la consumen el historial del admin y el del técnico.
 *
 * <p>Sustituye a {@code HistoryDto} sin tocar el JSON: mismos nombres de campo y
 * {@code status} sigue viajando como "completed"/"cancelled" (regla R1).
 */
public record ServiceRecordDto(
        Long id,
        String orderId,
        LocalDate completionDate,
        String completionTime,
        String client,
        String site,
        String serviceType,
        String technician,
        String technicianInitials,
        String status,
        Long workOrderId
) {
    public static ServiceRecordDto from(ServiceRecord r) {
        return new ServiceRecordDto(
                r.getId(),
                r.getOrderId(),
                r.getCompletionDate(),
                r.getCompletionTime(),
                r.getClient(),
                r.getSite(),
                r.getServiceType(),
                r.getTechnician(),
                r.getTechnicianInitials(),
                r.getStatus().persistedValue(),
                r.getWorkOrderId()
        );
    }
}
