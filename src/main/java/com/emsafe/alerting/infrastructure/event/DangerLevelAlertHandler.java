package com.emsafe.alerting.infrastructure.event;

import com.emsafe.alerting.application.AlarmApplicationService;
import com.emsafe.monitoring.domain.event.DangerLevelDetected;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Alerting reacciona a la telemetría: una lectura crítica levanta una alarma.
 *
 * <p><b>Esto es todo lo que hizo falta añadir.</b> Monitoring ya publicaba
 * {@code DangerLevelDetected} desde la fase 4 y no se ha tocado ni una línea suya: no
 * sabe que Alerting existe, ni que ahora alguien escucha. Ese es el argumento entero de
 * los eventos de dominio — una funcionalidad nueva que cruza dos contextos y que se
 * resuelve con un archivo y cero modificaciones en el emisor.
 *
 * <p><b>La fase importa: {@code BEFORE_COMMIT}.</b> Con la fase por defecto
 * ({@code AFTER_COMMIT}) la transacción de la ingesta ya está cerrada, un
 * {@code @Transactional} con propagación REQUIRED se engancha a ese contexto muerto y el
 * {@code save} se descarta <b>en silencio</b>. Costó un bug real en la fase 6 con las
 * alarmas de órdenes; aquí se aplica la lección desde el principio.
 *
 * <p>Aquí BEFORE_COMMIT además es lo correcto semánticamente: si la lectura no llega a
 * guardarse, la alarma que la anuncia tampoco debe existir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DangerLevelAlertHandler {

    private final AlarmApplicationService alarms;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DangerLevelDetected event) {
        boolean raised = alarms.raiseDangerDetected(
                event.serialNumber(),
                event.fieldUT(),
                event.clientId(),
                event.clientName());

        if (raised) {
            log.info("Alarma DANGER levantada para {} ({} µT)",
                    event.serialNumber(), event.fieldUT());
        } else {
            // Ruta normal, no un error: el sensor sigue en el mismo episodio.
            log.debug("Episodio DANGER ya abierto para {}, no se duplica la alarma",
                    event.serialNumber());
        }
    }
}
