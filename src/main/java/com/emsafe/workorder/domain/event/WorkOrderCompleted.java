package com.emsafe.workorder.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Una orden se completó.
 *
 * <p>Lo consume ServiceRecord para emitir el acta de servicio. Antes, WorkOrder
 * escribía directamente en el repositorio de History.
 *
 * <p>El {@link ClosedOrderSummary} viaja dentro del evento para que el suscriptor no
 * tenga que volver a cargar la orden.
 */
public record WorkOrderCompleted(Long workOrderId, String orderId, LocalDateTime completedAt,
                                 ClosedOrderSummary summary)
        implements DomainEvent {
}
