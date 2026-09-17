package com.emsafe.servicerecord.domain.model;

/**
 * Value Object: desenlace con el que se cerró una orden de trabajo.
 *
 * <p>Un acta de servicio solo existe para órdenes YA cerradas, así que su estado no
 * es el ciclo de vida completo de {@code WorkOrderStatus} — son exactamente dos
 * desenlaces. Antes era un {@code String} suelto que viajaba desde el service hasta
 * la columna sin que nada impidiera escribir "Completed", "done" o un typo.
 *
 * <p><b>Persistencia (regla R3):</b> se guardan los MISMOS literales que ya hay en
 * {@code history.status}. Sin migración Flyway.
 */
public enum ServiceRecordStatus {

    /** El técnico ejecutó el servicio. */
    COMPLETED("completed"),
    /** La orden se anuló antes de ejecutarse (lo que el admin ve como "eliminar"). */
    CANCELLED("cancelled");

    private final String persistedValue;

    ServiceRecordStatus(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    /** Literal exacto que viaja a la BD y al JSON del historial. */
    public String persistedValue() {
        return persistedValue;
    }

    /**
     * Lectura tolerante desde la BD: un valor nulo o desconocido se interpreta como
     * COMPLETED, que es el valor por defecto que tenía la columna.
     */
    public static ServiceRecordStatus fromPersisted(String value) {
        ServiceRecordStatus parsed = parse(value);
        return parsed != null ? parsed : COMPLETED;
    }

    /**
     * Parseo de un filtro de la API: devuelve {@code null} cuando no hay filtro o
     * cuando el valor no corresponde a ningún desenlace, para que el listado no
     * filtre por nada en vez de devolver vacío.
     */
    public static ServiceRecordStatus parseFilter(String value) {
        return parse(value);
    }

    private static ServiceRecordStatus parse(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase();
        for (ServiceRecordStatus status : values()) {
            if (status.persistedValue.equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
