package com.emsafe.servicerecord.infrastructure.event;

import com.emsafe.servicerecord.application.ServiceRecordApplicationService;
import com.emsafe.servicerecord.domain.model.ServiceRecordStatus;
import com.emsafe.workorder.domain.event.ClosedOrderSummary;
import com.emsafe.workorder.domain.event.WorkOrderCancelled;
import com.emsafe.workorder.domain.event.WorkOrderCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * El historial se escribe reaccionando al ciclo de vida de las órdenes.
 *
 * <p>Es la <b>capa anticorrupción</b> de ServiceRecord: el único punto del contexto
 * que conoce los tipos de WorkOrder. Traduce el evento publicado a la llamada del
 * caso de uso; ni el agregado ni el application service saben que WorkOrder existe.
 *
 * <p><b>{@code BEFORE_COMMIT} es obligatorio, no una preferencia.</b> Con la fase por
 * defecto ({@code AFTER_COMMIT}) la transacción original ya está cerrada y el
 * {@code save} se descarta en silencio — el mismo bug que se destapó en la fase 6 con
 * las alarmas. Así el acta y el cambio de estado de la orden se confirman juntos.
 */
@Component
@RequiredArgsConstructor
public class WorkOrderLifecycleHandler {

    private final ServiceRecordApplicationService serviceRecords;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(WorkOrderCompleted event) {
        record(event.workOrderId(), event.orderId(), event.summary(),
                ServiceRecordStatus.COMPLETED, event.completedAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(WorkOrderCancelled event) {
        record(event.workOrderId(), event.orderId(), event.summary(),
                ServiceRecordStatus.CANCELLED, LocalDateTime.now());
    }

    private void record(Long workOrderId, String orderId, ClosedOrderSummary summary,
                        ServiceRecordStatus status, LocalDateTime closedAt) {
        serviceRecords.recordClosure(
                workOrderId, orderId,
                summary.client(), summary.site(), summary.serviceType(),
                summary.technician(), summary.technicianInitials(), summary.technicianId(),
                status, closedAt);
    }
}
