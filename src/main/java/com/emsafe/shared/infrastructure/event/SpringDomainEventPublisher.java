package com.emsafe.shared.infrastructure.event;

import com.emsafe.shared.domain.event.DomainEvent;
import com.emsafe.shared.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link DomainEventPublisher} sobre el bus de eventos de Spring.
 *
 * <p>Se apoya en {@code ApplicationEventPublisher} para no añadir dependencias nuevas.
 * Los suscriptores usan {@code @TransactionalEventListener}, de modo que solo reaccionan
 * si la transacción que produjo el hecho realmente se confirmó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {}", event.getClass().getSimpleName());
        delegate.publishEvent(event);
    }
}
