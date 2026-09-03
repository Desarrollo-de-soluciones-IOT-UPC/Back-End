package com.emsafe.device.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/** Un sensor fue recolectado y devuelto al pool: queda libre para otra instalación. */
public record DeviceReleased(Long deviceId, String serialNumber, Long previousClientId) implements DomainEvent {
}
