package com.emsafe.device.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;

/**
 * Value Object: estado del ciclo de vida de un sensor EMSafe.
 *
 * <p>Antes era un {@code String} suelto comparado a mano contra literales
 * (`"unregistered".equals(...)`, `"requires-maintenance"`) repartidos por
 * DeviceService, WorkOrderService y TelemetryService. Aquí queda documentado
 * el ciclo completo, que es el del negocio de campo:
 *
 * <pre>
 *   UNREGISTERED ──(instalación: el técnico lo reclama)──▶ ACTIVE
 *        ▲                                                   │
 *        │                                        (avería detectada)
 *   (recolección: vuelve al pool)                            ▼
 *        │                                        REQUIRES_MAINTENANCE
 *        │                                                   │
 *        └───────────────── ACTIVE ◀──(fin)── IN_MAINTENANCE ◀┘
 * </pre>
 *
 * <p><b>Persistencia (regla R3):</b> se guardan los MISMOS literales que ya hay
 * en la columna {@code devices.status}. Sin migración Flyway.
 */
public enum DeviceStatus {

    /** Operativo y asignado a un cliente. */
    ACTIVE("active"),
    /** Dado de baja temporalmente por un administrador. */
    INACTIVE("inactive"),
    /** Descubierto por el edge pero aún sin cliente asignado (pool). */
    UNREGISTERED("unregistered"),
    /** Marcado como averiado; espera una orden de mantenimiento. */
    REQUIRES_MAINTENANCE("requires-maintenance"),
    /** Un técnico está trabajando en él ahora mismo. */
    IN_MAINTENANCE("in-maintenance");

    private final String persistedValue;

    DeviceStatus(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    /** Literal exacto que viaja a la BD y a la API. */
    public String persistedValue() {
        return persistedValue;
    }

    public boolean isUnregistered() {
        return this == UNREGISTERED;
    }

    public boolean needsMaintenance() {
        return this == REQUIRES_MAINTENANCE;
    }

    /**
     * Lectura tolerante desde la BD: un valor nulo o desconocido se interpreta
     * como ACTIVE, para que datos legacy nunca rompan el arranque.
     */
    public static DeviceStatus fromPersisted(String value) {
        DeviceStatus parsed = parse(value);
        return parsed != null ? parsed : ACTIVE;
    }

    /** Parseo estricto para entradas de la API: rechaza estados inventados con un 400. */
    public static DeviceStatus fromApi(String value) {
        DeviceStatus parsed = parse(value);
        if (parsed == null) {
            throw new BadRequestException("Invalid device status: " + value);
        }
        return parsed;
    }

    private static DeviceStatus parse(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase();
        for (DeviceStatus status : values()) {
            if (status.persistedValue.equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
