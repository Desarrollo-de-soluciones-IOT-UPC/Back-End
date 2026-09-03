package com.emsafe.shared.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: dirección de correo, identidad de login de un usuario EMSafe.
 *
 * <p>Invariantes: formato válido y normalización a minúsculas — así
 * {@code Admin@EMSafe.com} y {@code admin@emsafe.com} son el mismo usuario.
 */
@Embeddable
public final class Email {

    private static final Pattern FORMAT =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private String value;

    /** Requerido por JPA. */
    protected Email() {
    }

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        String normalized = raw.trim().toLowerCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        return new Email(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email other)) return false;
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
