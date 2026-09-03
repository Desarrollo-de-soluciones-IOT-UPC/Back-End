package com.emsafe.shared.domain.event;

/**
 * Puerto de salida para publicar eventos de dominio.
 *
 * <p>Los application services del dominio dependen de esta interfaz, no de Spring.
 * El adaptador vive en {@code shared/infrastructure/event/SpringDomainEventPublisher}.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
