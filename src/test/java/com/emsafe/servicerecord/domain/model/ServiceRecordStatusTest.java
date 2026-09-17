package com.emsafe.servicerecord.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VO {@link ServiceRecordStatus}: los dos desenlaces posibles de una orden cerrada.
 *
 * <p>No es el ciclo de vida completo de {@code WorkOrderStatus}: un acta sólo existe
 * para órdenes YA cerradas, así que "pending" o "in-progress" no tienen sentido aquí.
 */
class ServiceRecordStatusTest {

    @Test
    void los_literales_persistidos_son_los_de_siempre() {
        assertThat(ServiceRecordStatus.COMPLETED.persistedValue()).isEqualTo("completed");
        assertThat(ServiceRecordStatus.CANCELLED.persistedValue()).isEqualTo("cancelled");
    }

    @Test
    void solo_existen_dos_desenlaces() {
        assertThat(ServiceRecordStatus.values()).hasSize(2);
    }

    @Test
    void la_lectura_desde_la_bd_es_tolerante() {
        assertThat(ServiceRecordStatus.fromPersisted("cancelled")).isEqualTo(ServiceRecordStatus.CANCELLED);
        assertThat(ServiceRecordStatus.fromPersisted("COMPLETED")).isEqualTo(ServiceRecordStatus.COMPLETED);
        assertThat(ServiceRecordStatus.fromPersisted(null)).isEqualTo(ServiceRecordStatus.COMPLETED);
        assertThat(ServiceRecordStatus.fromPersisted("basura")).isEqualTo(ServiceRecordStatus.COMPLETED);
    }

    @Test
    void un_filtro_desconocido_no_filtra_en_vez_de_devolver_vacio() {
        // Cambio de comportamiento consciente de la fase 7: antes un ?status= inválido
        // devolvía lista vacía; ahora se ignora el filtro (igual que WorkOrderStatus).
        assertThat(ServiceRecordStatus.parseFilter("completed")).isEqualTo(ServiceRecordStatus.COMPLETED);
        assertThat(ServiceRecordStatus.parseFilter("noexiste")).isNull();
        assertThat(ServiceRecordStatus.parseFilter(null)).isNull();
        assertThat(ServiceRecordStatus.parseFilter("  ")).isNull();
    }
}
