package com.emsafe.device.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VO {@link DeviceStatus}.
 *
 * <p>Los literales persistidos son los que ya estaban en la columna {@code devices.status}
 * antes del refactor. Cambiarlos rompería los datos existentes sin una migración.
 */
class DeviceStatusTest {

    @Test
    void los_literales_persistidos_son_los_de_siempre() {
        // Regla R3 del plan DDD: el converter escribe estos strings, no el nombre del enum.
        assertThat(DeviceStatus.ACTIVE.persistedValue()).isEqualTo("active");
        assertThat(DeviceStatus.INACTIVE.persistedValue()).isEqualTo("inactive");
        assertThat(DeviceStatus.UNREGISTERED.persistedValue()).isEqualTo("unregistered");
        assertThat(DeviceStatus.REQUIRES_MAINTENANCE.persistedValue()).isEqualTo("requires-maintenance");
        assertThat(DeviceStatus.IN_MAINTENANCE.persistedValue()).isEqualTo("in-maintenance");
    }

    @Test
    void la_lectura_desde_la_bd_es_tolerante() {
        // Un valor raro en la columna no puede impedir que la app arranque.
        assertThat(DeviceStatus.fromPersisted("unregistered")).isEqualTo(DeviceStatus.UNREGISTERED);
        assertThat(DeviceStatus.fromPersisted("ACTIVE")).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(DeviceStatus.fromPersisted(null)).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(DeviceStatus.fromPersisted("basura")).isEqualTo(DeviceStatus.ACTIVE);
    }

    @Test
    void la_entrada_de_la_api_es_estricta() {
        // Aquí sí: un estado inventado que llega por REST es un 400.
        assertThat(DeviceStatus.fromApi("in-maintenance")).isEqualTo(DeviceStatus.IN_MAINTENANCE);
        assertThatThrownBy(() -> DeviceStatus.fromApi("teleported"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> DeviceStatus.fromApi(null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void preguntas_de_negocio() {
        assertThat(DeviceStatus.UNREGISTERED.isUnregistered()).isTrue();
        assertThat(DeviceStatus.ACTIVE.isUnregistered()).isFalse();
        assertThat(DeviceStatus.REQUIRES_MAINTENANCE.needsMaintenance()).isTrue();
        assertThat(DeviceStatus.IN_MAINTENANCE.needsMaintenance()).isFalse();
    }
}
