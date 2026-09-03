package com.emsafe.history.infrastructure.event;

import com.emsafe.history.service.HistoryService;
import com.emsafe.workorder.domain.event.WorkOrderCancelled;
import com.emsafe.workorder.domain.event.WorkOrderCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * El historial se escribe reaccionando al ciclo de vida de las órdenes.
 *
 * <p>Antes, {@code WorkOrderService} construía la entidad {@code History} y la
 * guardaba él mismo, conociendo el formato de la hora y la representación del tipo.
 * Ahora ese conocimiento vive donde pertenece.
 *
 * <p>{@code BEFORE_COMMIT} para que el acta y el cambio de estado de la orden se
 * confirmen juntos.
 */
@Component
@RequiredArgsConstructor
public class WorkOrderLifecycleHandler {

    private final HistoryService historyService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(WorkOrderCompleted event) {
        historyService.recordFromWorkOrder(event.workOrderId(), "completed");
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(WorkOrderCancelled event) {
        historyService.recordFromWorkOrder(event.workOrderId(), "cancelled");
    }
}
