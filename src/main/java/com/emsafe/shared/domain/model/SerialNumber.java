package com.emsafe.shared.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value Object: número de serie de un sensor EMSafe (p. ej. {@code EMSAFE-6766-01}).
 *
 * <p>Pertenece al Shared Kernel porque es el identificador con el que los contextos
 * Device y Monitoring se refieren al mismo sensor físico, y el que viaja desde el
 * firmware ESP32 → edge → backend.
 *
 * <p>Invariante: no puede ser nulo ni vacío; se normaliza recortando espacios.
 */
@Embeddable
public final class SerialNumber {

    private String value;

    /** Requerido por JPA. */
    protected SerialNumber() {
    }

    private SerialNumber(String value) {
        this.value = value;
    }

    public static SerialNumber of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("serialNumber is required");
        }
        return new SerialNumber(raw.trim());
    }

    /** Versión tolerante para datos legacy: devuelve null en vez de lanzar. */
    public static SerialNumber ofNullable(String raw) {
        return (raw == null || raw.isBlank()) ? null : new SerialNumber(raw.trim());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialNumber other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
