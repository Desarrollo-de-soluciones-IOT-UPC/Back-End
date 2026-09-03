package com.emsafe.workorder.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/**
 * Se creó una orden de trabajo.
 *
 * <p>Lo consume Alerting para levantar la alarma informativa. Gracias a este evento,
 * WorkOrder ya no conoce el contexto Alerting.
 */
public record WorkOrderCreated(Long workOrderId, String orderId, String clientDisplayName,
                               String technicianName, Long clientUserId) implements DomainEvent {
}
