package com.emsafe.alerting.infrastructure.event;

import com.emsafe.alerting.application.AlarmApplicationService;
import com.emsafe.workorder.domain.event.WorkOrderCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Alerting reacciona a los hechos del contexto WorkOrder.
 *
 * <p><b>Ojo con la fase.</b> Tiene que ser {@code BEFORE_COMMIT}, no la fase por
 * defecto ({@code AFTER_COMMIT}): en AFTER_COMMIT la transacción original ya está
 * cerrada, y un {@code @Transactional} con propagación REQUIRED se engancha a ese
 * contexto muerto en lugar de abrir uno nuevo, con lo que el {@code save} se
 * descarta <b>en silencio</b>. Se detectó exactamente así — el acta del historial
 * (BEFORE_COMMIT) se escribía y la alarma no.
 *
 * <p>Además, BEFORE_COMMIT conserva la semántica original: la alarma se guardaba en
 * la MISMA transacción que la orden, así que o se crean las dos o ninguna.
 */
@Component
@RequiredArgsConstructor
public class WorkOrderAlertHandler {

    private final AlarmApplicationService alarms;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(WorkOrderCreated event) {
        alarms.raiseOrderCreated(
                event.orderId(),
                event.clientDisplayName(),
                event.technicianName(),
                event.clientUserId());
    }
}
