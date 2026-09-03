package com.emsafe.iam.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;

/**
 * Value Object: rol de un usuario dentro de EMSafe.
 *
 * <p>Se persiste como {@code EnumType.STRING} ("ADMIN"/"TECHNICIAN"/"CLIENT"), pero la API
 * lo expone en dos formatos históricos que el front ya consume y que NO se pueden cambiar:
 * <ul>
 *   <li>{@link #apiValue()} — minúscula, usado por el login y los DTOs de cliente.</li>
 *   <li>{@link #displayValue()} — Title Case, usado por {@code UserDto} en el portal web.</li>
 * </ul>
 */
public enum Role {

    ADMIN,
    TECHNICIAN,
    CLIENT;

    /** Formato del login y de los DTOs de cliente ("admin", "technician", "client"). */
    public String apiValue() {
        return name().toLowerCase();
    }

    /** Formato Title Case que espera el portal Angular ("Admin", "Technician", "Client"). */
    public String displayValue() {
        String n = name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    /** Parseo estricto para filtros de la API; lanza 400 si no se reconoce. */
    public static Role fromApi(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Role is required");
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + value);
        }
    }
}
