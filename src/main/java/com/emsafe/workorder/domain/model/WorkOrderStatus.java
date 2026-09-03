package com.emsafe.workorder.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;

/**
 * Value Object: estado de una orden de trabajo, con su máquina de estados.
 *
 * <pre>
 *   PENDING ──▶ IN_PROGRESS ──▶ COMPLETED
 *      │              │
 *      └──────────────┴──────▶ CANCELLED
 *
 *   COMPLETED y CANCELLED son terminales: de ahí no se sale.
 * </pre>
 *
 * <p>La regla de transición vivía en un método privado de {@code WorkOrderService};
 * al traerla aquí, ninguna ruta de código puede saltársela.
 */
public enum WorkOrderStatus {

    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /** Estados que siguen "vivos" y aparecen en las listas de admin y técnico. */
    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS;
    }

    /** Una orden terminal ya no admite cambios de estado. */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** ¿Es válido pasar de este estado al indicado? */
    public boolean canTransitionTo(WorkOrderStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /**
     * Parseo laxo para filtros de listado: {@code null}, vacío o "all" significan
     * "sin filtro" y devuelven null. Un valor no reconocido sí es un 400.
     */
    public static WorkOrderStatus parseFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return fromApi(value);
    }

    /** Parseo estricto: acepta "in-progress" y "IN_PROGRESS"; lanza 400 si no existe. */
    public static WorkOrderStatus fromApi(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        try {
            return WorkOrderStatus.valueOf(value.trim().toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + value);
        }
    }
}
