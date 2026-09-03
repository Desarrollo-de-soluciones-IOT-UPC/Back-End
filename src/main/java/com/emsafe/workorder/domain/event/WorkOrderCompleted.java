package com.emsafe.workorder.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Una orden se completó.
 *
 * <p>Lo consume ServiceRecord para escribir el acta de servicio. Antes, WorkOrder
 * escribía directamente en el repositorio de History.
 */
public record WorkOrderCompleted(Long workOrderId, String orderId, LocalDateTime completedAt)
        implements DomainEvent {
}
