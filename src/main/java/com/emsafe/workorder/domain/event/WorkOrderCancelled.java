package com.emsafe.workorder.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/** Una orden se canceló (lo que el admin ve como "eliminar"). Lo consume ServiceRecord. */
public record WorkOrderCancelled(Long workOrderId, String orderId, String reason)
        implements DomainEvent {
}
