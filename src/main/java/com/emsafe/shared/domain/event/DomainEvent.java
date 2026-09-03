package com.emsafe.shared.domain.event;

import java.time.Instant;

/**
 * Marcador de los eventos de dominio de EMSafe.
 *
 * <p>Un evento describe <b>algo que ya ocurrió</b> en un bounded context y que
 * puede interesar a otros (nombre en pasado: {@code WorkOrderCompleted},
 * {@code ReadingIngested}). Es el mecanismo con el que se rompe el acoplamiento
 * directo entre contextos: en vez de que WorkOrder llame a los repositorios de
 * Alerting, Device e History, publica un evento y ellos reaccionan.
 */
public interface DomainEvent {

    /** Momento en que ocurrió el hecho. */
    default Instant occurredOn() {
        return Instant.now();
    }
}
