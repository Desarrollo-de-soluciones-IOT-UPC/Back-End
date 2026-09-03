package com.emsafe.device.domain.event;

import com.emsafe.device.domain.model.PlugState;
import com.emsafe.shared.domain.event.DomainEvent;

/**
 * El usuario ordenó abrir o cortar la corriente de su sensor desde la app móvil.
 * El edge recogerá la orden en su siguiente consulta a {@code /api/v1/devices/{serial}/plug}.
 */
public record PlugOrdered(Long deviceId, String serialNumber, PlugState desired) implements DomainEvent {
}
