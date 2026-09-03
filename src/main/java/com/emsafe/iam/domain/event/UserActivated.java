package com.emsafe.iam.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/** Un administrador aprobó/reactivó una cuenta: ya puede iniciar sesión. */
public record UserActivated(Long userId, String email) implements DomainEvent {
}
