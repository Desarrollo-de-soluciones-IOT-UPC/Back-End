package com.emsafe.workorder.domain.model;

/**
 * Value Object: el identificador legible de una orden ({@code #WO-0042}).
 *
 * <p>El formato estaba incrustado como un {@code String.format} suelto dentro de
 * {@code WorkOrderService.create()}. Aquí queda documentado y en un solo sitio: se
 * deriva de la identidad numérica que asigna la base de datos, por eso solo puede
 * calcularse DESPUÉS del primer save.
 */
public final class OrderId {

    private static final String FORMAT = "#WO-%04d";

    private OrderId() {
    }

    /** Construye el código legible a partir del id autogenerado. */
    public static String forSequence(Long sequence) {
        return String.format(FORMAT, sequence);
    }
}
