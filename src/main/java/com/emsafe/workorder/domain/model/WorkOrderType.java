package com.emsafe.workorder.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;

/**
 * Value Object: tipo de trabajo de campo. Determina qué le pasa a los sensores
 * cuando la orden avanza.
 *
 * <ul>
 *   <li>{@link #INSTALLATION} — el técnico reclama sensores del pool para el cliente.</li>
 *   <li>{@link #MAINTENANCE} — al arrancar, los sensores averiados del cliente pasan a
 *       "in-maintenance".</li>
 *   <li>{@link #COLLECTION} — los sensores vuelven al pool.</li>
 * </ul>
 */
public enum WorkOrderType {

    INSTALLATION,
    MAINTENANCE,
    COLLECTION;

    /** Nombre en Title Case que se guarda en el historial ("Installation"). */
    public String displayValue() {
        String n = name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    /** Parseo laxo para filtros: null/vacío/"all" ⇒ sin filtro. */
    public static WorkOrderType parseFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return fromApi(value);
    }

    public static WorkOrderType fromApi(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Type is required");
        }
        try {
            return WorkOrderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid type: " + value);
        }
    }
}
