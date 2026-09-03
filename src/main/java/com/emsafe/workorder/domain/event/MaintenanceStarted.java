package com.emsafe.workorder.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/**
 * Arrancó una orden de mantenimiento.
 *
 * <p>Lo consume Device para pasar a "in-maintenance" los sensores del cliente que
 * estaban marcados como averiados — sin que WorkOrder toque el repositorio de Device.
 */
public record MaintenanceStarted(Long workOrderId, Long clientUserId) implements DomainEvent {
}
