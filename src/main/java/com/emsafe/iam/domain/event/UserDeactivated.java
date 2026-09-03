package com.emsafe.iam.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/** Una cuenta fue desactivada: su JWT deja de ser operativo en el siguiente filtro. */
public record UserDeactivated(Long userId, String email) implements DomainEvent {
}
