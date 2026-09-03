package com.emsafe.iam.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/**
 * Una cuenta fue eliminada.
 *
 * <p>Device escuchará este evento para liberar los sensores del cliente en vez de
 * dejarlos huérfanos (hoy la FK {@code devices.client_id} queda en SET NULL).
 */
public record UserDeleted(Long userId, String email) implements DomainEvent {
}
