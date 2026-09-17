package com.emsafe.reporting.interfaces.rest.dto;

import com.emsafe.workorder.interfaces.rest.dto.WorkOrderDto;

import java.time.LocalDate;

/**
 * Fila del panel "últimas órdenes" del dashboard.
 *
 * <p>Se proyecta desde el {@code WorkOrderDto} que publica el contexto WorkOrder, no
 * desde su agregado: Reporting es un read model y no debe tocar los agregados ajenos.
 */
public record LatestWorkOrderDto(
        Long id,
        String orderId,
        String siteLocation,
        String technician,
        String status,
        LocalDate date
) {
    public static LatestWorkOrderDto from(WorkOrderDto wo) {
        String site = wo.location() != null && wo.city() != null
                ? wo.location() + " — " + wo.city()
                : (wo.location() != null ? wo.location() : wo.city());

        return new LatestWorkOrderDto(
                wo.id(),
                wo.orderId(),
                site,
                wo.technician(),
                wo.status(),
                wo.date()
        );
    }
}
