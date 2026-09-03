package com.emsafe.workorder.domain.model;

/**
 * Value Object: prioridad de una orden de trabajo.
 *
 * <p>La columna {@code work_orders.priority} guarda texto libre en Title Case
 * ("High", "Medium", "Low"...) y el front lo muestra tal cual, así que aquí NO se
 * usa un enum con converter: normalizarlo cambiaría el JSON (regla R1).
 *
 * <p>Lo que sí aporta este VO es un sitio único para responder "¿es urgente?",
 * que antes no existía en ninguna parte.
 */
public final class Priority {

    public static final String HIGH = "High";
    public static final String MEDIUM = "Medium";
    public static final String LOW = "Low";

    private Priority() {
    }

    public static boolean isUrgent(String priority) {
        return priority != null && HIGH.equalsIgnoreCase(priority.trim());
    }
}
