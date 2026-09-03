package com.emsafe.monitoring.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/**
 * Una lectura superó el umbral crítico (DANGER).
 *
 * <p>Es el gancho natural para que el contexto Alerting levante una alarma sin que
 * Monitoring tenga que conocerlo.
 */
public record DangerLevelDetected(Long readingId, String serialNumber, Double fieldUT,
                                  Long clientId, String clientName) implements DomainEvent {
}
