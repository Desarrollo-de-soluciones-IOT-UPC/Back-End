package com.emsafe.device.domain.model;

import com.emsafe.iam.domain.model.User;
import com.emsafe.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El agregado {@link Device}: instalación, recolección y mantenimiento de un sensor.
 *
 * <p>La regla que más importa aquí: <b>devolver un sensor al pool implica limpiar su
 * cliente y su fecha de instalación</b>. Estaba duplicada a mano en
 * {@code WorkOrderService} y en {@code DeviceService}, y era fácil hacer sólo la mitad.
 */
class DeviceTest {

    private static User cliente(Long id, String nombre) {
        return User.builder().id(id).name(nombre).build();
    }

    @Nested
    @DisplayName("Descubrimiento por el edge")
    class Descubrimiento {

        @Test
        void un_sensor_desconocido_se_autocrea_en_el_pool() {
            // Pasa cuando el edge reporta un serial que el backend no conoce:
            // se crea sin cliente para no perder la telemetría.
            Device d = Device.discoveredByEdge("EMSAFE-6766-01");

            assertThat(d.getStatus()).isEqualTo(DeviceStatus.UNREGISTERED);
            assertThat(d.isUnregistered()).isTrue();
            assertThat(d.getClient()).isNull();
            assertThat(d.getSerialNumber()).isEqualTo("EMSAFE-6766-01");
        }

        @Test
        void el_serial_se_normaliza() {
            assertThat(Device.discoveredByEdge("  EMSAFE-6766-01  ").getSerialNumber())
                    .isEqualTo("EMSAFE-6766-01");
        }

        @Test
        void sin_serial_no_hay_sensor() {
            assertThatThrownBy(() -> Device.discoveredByEdge(null))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> Device.discoveredByEdge("   "))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Instalación: el técnico reclama un sensor")
    class Instalacion {

        @Test
        void reclamar_lo_deja_operativo_en_la_sede_del_cliente() {
            Device d = Device.discoveredByEdge("EMSAFE-6766-01");

            d.claimFor(cliente(8L, "Quantum Dynamics"), "Sala de máquinas", "Sensor Norte", "Sensor");

            assertThat(d.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
            assertThat(d.getName()).isEqualTo("Sensor Norte");
            assertThat(d.getLocation()).isEqualTo("Sala de máquinas");
            assertThat(d.getInstallDate()).isEqualTo(LocalDate.now());
            assertThat(d.belongsTo(8L)).isTrue();
        }

        @Test
        void si_no_se_da_nombre_se_conserva_el_que_tenia() {
            Device d = Device.discoveredByEdge("EMSAFE-6766-01");
            String nombreAuto = d.getName();

            d.claimFor(cliente(8L, "Quantum"), "Sala", null, null);

            assertThat(d.getName()).isEqualTo(nombreAuto);
            assertThat(d.getType()).isEqualTo("Sensor");
        }

        @Test
        void un_sensor_es_de_su_cliente_y_de_nadie_mas() {
            Device d = Device.discoveredByEdge("EMSAFE-6766-01");
            d.claimFor(cliente(8L, "Quantum"), "Sala", null, null);

            assertThat(d.belongsTo(8L)).isTrue();
            assertThat(d.belongsTo(9L)).isFalse();
            assertThat(d.belongsTo(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Recolección: el sensor vuelve al pool")
    class Recoleccion {

        @Test
        void devolver_al_pool_limpia_cliente_y_fecha() {
            // La regla que estaba duplicada a mano en dos services.
            Device d = Device.register("Sensor Norte", "Sensor", "Sala",
                    DeviceStatus.ACTIVE, "EMSAFE-1", LocalDate.now(), cliente(8L, "Quantum"));

            d.releaseToPool();

            assertThat(d.getStatus()).isEqualTo(DeviceStatus.UNREGISTERED);
            assertThat(d.getClient()).isNull();
            assertThat(d.getInstallDate()).isNull();
        }

        @Test
        void cambiar_el_estado_a_unregistered_tambien_lo_devuelve_al_pool() {
            // Nadie puede saltarse la regla pasando por la puerta genérica.
            Device d = Device.register("Sensor Norte", "Sensor", "Sala",
                    DeviceStatus.ACTIVE, "EMSAFE-1", LocalDate.now(), cliente(8L, "Quantum"));

            d.changeStatus(DeviceStatus.UNREGISTERED);

            assertThat(d.getClient()).isNull();
            assertThat(d.getInstallDate()).isNull();
        }
    }

    @Nested
    @DisplayName("Mantenimiento")
    class Mantenimiento {

        @Test
        void marcar_averiado_y_arrancar_el_trabajo() {
            Device d = Device.discoveredByEdge("EMSAFE-1");

            d.flagForMaintenance();
            assertThat(d.getStatus()).isEqualTo(DeviceStatus.REQUIRES_MAINTENANCE);
            assertThat(d.getStatus().needsMaintenance()).isTrue();

            d.startMaintenance();
            assertThat(d.getStatus()).isEqualTo(DeviceStatus.IN_MAINTENANCE);
        }

        @Test
        void un_estado_nulo_es_un_400() {
            assertThatThrownBy(() -> Device.discoveredByEdge("EMSAFE-1").changeStatus(null))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Orden del relé")
    class Rele {

        @Test
        void se_guarda_el_estado_deseado_por_el_usuario() {
            Device d = Device.discoveredByEdge("EMSAFE-1");

            d.orderPlug(PlugState.OFF);
            assertThat(d.getDesiredPlug()).isEqualTo(PlugState.OFF);

            d.orderPlug(PlugState.ON);
            assertThat(d.getDesiredPlug()).isEqualTo(PlugState.ON);
        }

        @Test
        void se_admite_null_como_sin_orden() {
            Device d = Device.discoveredByEdge("EMSAFE-1");
            d.orderPlug(PlugState.ON);

            d.orderPlug(null);

            assertThat(d.getDesiredPlug()).isNull();
        }
    }
}
