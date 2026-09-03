package com.emsafe.iam.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/**
 * Un cliente se registró desde la app móvil y quedó a la espera de aprobación.
 *
 * <p>Lo consumirá Alerting para avisar al administrador de que hay una cuenta pendiente.
 */
public record UserRegistered(Long userId, String email, String name) implements DomainEvent {
}
