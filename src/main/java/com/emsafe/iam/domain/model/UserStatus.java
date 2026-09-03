package com.emsafe.iam.domain.model;

/**
 * Value Object: estado del ciclo de vida de una cuenta.
 *
 * <p>Sustituye al {@code String status} suelto que se comparaba con
 * {@code "active".equalsIgnoreCase(...)} en cinco sitios distintos.
 *
 * <p><b>Contrato de persistencia (regla R3 del plan):</b> se guarda con el MISMO
 * literal en minúscula que ya está en la base de datos ({@code active}, {@code pending},
 * {@code inactive}). No hay migración Flyway asociada.
 *
 * <p>Compatibilidad legacy: un status nulo o desconocido se interpreta como
 * {@link #ACTIVE}, exactamente como hacía el código anterior
 * ({@code status != null && !"active".equalsIgnoreCase(status)}).
 */
public enum UserStatus {

    /** Cuenta operativa: es la única que puede iniciar sesión. */
    ACTIVE("active"),
    /** Registro público a la espera de aprobación de un administrador. */
    PENDING("pending"),
    /** Cuenta desactivada por un administrador. */
    INACTIVE("inactive");

    private final String persistedValue;

    UserStatus(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    /** Literal exacto que viaja a la BD y a la API. */
    public String persistedValue() {
        return persistedValue;
    }

    public boolean canSignIn() {
        return this == ACTIVE;
    }

    /** Tolerante por diseño: null o valor desconocido ⇒ ACTIVE (semántica legacy). */
    public static UserStatus fromPersisted(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        String normalized = value.trim().toLowerCase();
        for (UserStatus status : values()) {
            if (status.persistedValue.equals(normalized)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
