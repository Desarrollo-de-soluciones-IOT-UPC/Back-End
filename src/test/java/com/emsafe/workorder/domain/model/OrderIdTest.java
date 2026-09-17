package com.emsafe.workorder.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VO {@link OrderId}: el código legible de una orden.
 *
 * <p>Era un {@code String.format} suelto dentro de {@code WorkOrderService.create()}.
 * El front lo muestra tal cual y el historial lo usa para buscar, así que el formato
 * es parte del contrato.
 */
class OrderIdTest {

    @Test
    void el_formato_es_WO_con_cuatro_digitos() {
        assertThat(OrderId.forSequence(1L)).isEqualTo("#WO-0001");
        assertThat(OrderId.forSequence(42L)).isEqualTo("#WO-0042");
        assertThat(OrderId.forSequence(9999L)).isEqualTo("#WO-9999");
    }

    @Test
    void pasados_los_cuatro_digitos_no_se_trunca() {
        assertThat(OrderId.forSequence(12345L)).isEqualTo("#WO-12345");
    }
}
