package com.emsafe.monitoring.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/** Llegó una lectura nueva desde el edge y quedó persistida. */
public record ReadingIngested(Long readingId, String serialNumber, Double fieldUT,
                              String level, Long clientId) implements DomainEvent {
}
