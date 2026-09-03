package com.emsafe.device.domain.event;

import com.emsafe.shared.domain.event.DomainEvent;

/** Un técnico reclamó un sensor del pool y lo dejó operativo en la sede de un cliente. */
public record DeviceClaimed(Long deviceId, String serialNumber, Long clientId) implements DomainEvent {
}
