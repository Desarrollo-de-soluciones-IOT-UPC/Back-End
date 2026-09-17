package com.emsafe.workorder.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/**
 * Una orden se canceló (lo que el admin ve como "eliminar"). Lo consume ServiceRecord,
 * que emite el acta con el desenlace {@code cancelled}.
 */
public record WorkOrderCancelled(Long workOrderId, String orderId, String reason,
                                 ClosedOrderSummary summary)
        implements DomainEvent {
}
