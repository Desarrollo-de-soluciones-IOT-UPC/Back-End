package com.emsafe.device.infrastructure.event;

import com.emsafe.device.application.DeviceApplicationService;
import com.emsafe.workorder.domain.event.MaintenanceStarted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Device reacciona al arranque de una orden de mantenimiento pasando a
 * "in-maintenance" los sensores del cliente que estaban marcados como averiados.
 *
 * <p>Antes, {@code WorkOrderService} consultaba y guardaba directamente en el
 * repositorio de Device. Con este suscriptor, cada contexto vuelve a ser dueño de
 * sus propios datos.
 *
 * <p>Se usa {@code BEFORE_COMMIT} para que el cambio de los sensores participe en la
 * MISMA transacción que el parte del técnico: o se guardan las dos cosas, o ninguna.
 */
@Component
@RequiredArgsConstructor
public class MaintenanceStartedHandler {

    private final DeviceApplicationService devices;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(MaintenanceStarted event) {
        if (event.clientUserId() != null) {
            devices.startMaintenanceForClient(event.clientUserId());
        }
    }
}
