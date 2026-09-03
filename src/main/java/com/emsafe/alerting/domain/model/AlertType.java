package com.emsafe.alerting.domain.model;

/**
 * Value Object: severidad de una alarma. Determina el color y el icono en el portal.
 *
 * <p>Persistido con los mismos literales en minúscula de siempre (columna
 * {@code alerts.type}); el dashboard cuenta las críticas con {@code countByType("danger")}.
 */
public enum AlertType {

    DANGER("danger"),
    WARNING("warning"),
    INFO("info"),
    SUCCESS("success");

    private final String persistedValue;

    AlertType(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    public String persistedValue() {
        return persistedValue;
    }

    public boolean isCritical() {
        return this == DANGER;
    }

    /** Tolerante: null o desconocido ⇒ INFO (la severidad más baja, nunca alarma de más). */
    public static AlertType fromPersisted(String value) {
        if (value == null || value.isBlank()) return INFO;
        String normalized = value.trim().toLowerCase();
        for (AlertType type : values()) {
            if (type.persistedValue.equals(normalized)) {
                return type;
            }
        }
        return INFO;
    }
}
