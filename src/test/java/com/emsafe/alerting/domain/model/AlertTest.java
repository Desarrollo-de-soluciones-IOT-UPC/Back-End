package com.emsafe.alerting.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El agregado {@link Alert}.
 *
 * <p><b>La invariante:</b> una alarma va a TODOS los clientes o a un subconjunto
 * CONCRETO. Antes eso se mantenía a mano en tres pasos separados (poner
 * {@code recipientType}, rellenar la lista de ids y denormalizar el nombre) y era
 * fácil dejarlos incoherentes — de hecho {@code WorkOrderService} tenía su propia copia.
 */
class AlertTest {

    @Nested
    @DisplayName("Alarma general")
    class ParaTodos {

        @Test
        void va_dirigida_a_todos_los_clientes() {
            Alert a = Alert.forAllClients(AlertType.INFO, "ph-bell", "Mantenimiento programado",
                    "El domingo a las 9", "Just now", null);

            assertThat(a.isForAllClients()).isTrue();
            assertThat(a.getRecipientClientIds()).isEmpty();
            assertThat(a.getClientName()).isEqualTo("All clients");
        }

        @Test
        void la_ve_cualquier_cliente() {
            Alert a = Alert.forAllClients(AlertType.INFO, "ph-bell", "Aviso", null, null, null);

            assertThat(a.isVisibleTo(8L)).isTrue();
            assertThat(a.isVisibleTo(99L)).isTrue();
        }
    }

    @Nested
    @DisplayName("Alarma dirigida")
    class ParaClientesConcretos {

        @Test
        void mantiene_coherentes_destinatarios_ids_y_nombres() {
            Alert a = Alert.forClients(AlertType.DANGER, "ph-warning", "Nivel crítico",
                    "Sensor Norte a 250 µT", "Just now", "EMSAFE-1",
                    List.of(8L, 9L), "Quantum Dynamics, Harbor Medical");

            assertThat(a.isForAllClients()).isFalse();
            assertThat(a.getRecipientClientIds()).containsExactly(8L, 9L);
            assertThat(a.getClientName()).isEqualTo("Quantum Dynamics, Harbor Medical");
        }

        @Test
        void solo_la_ven_sus_destinatarios() {
            Alert a = Alert.forClients(AlertType.DANGER, "ph-warning", "Nivel crítico",
                    null, null, null, List.of(8L), "Quantum");

            assertThat(a.isVisibleTo(8L)).isTrue();
            assertThat(a.isVisibleTo(9L)).isFalse();
            assertThat(a.isVisibleTo(null)).isFalse();
        }

        @Test
        void una_lista_vacia_degrada_a_alarma_general() {
            // "Específica para nadie" no significaría nada: se convierte en general.
            Alert a = Alert.forClients(AlertType.INFO, "ph-bell", "Aviso",
                    null, null, null, List.of(), "");

            assertThat(a.isForAllClients()).isTrue();
            assertThat(a.getClientName()).isEqualTo("All clients");
        }

        @Test
        void una_lista_nula_tambien_degrada() {
            Alert a = Alert.forClients(AlertType.INFO, "ph-bell", "Aviso",
                    null, null, null, null, null);

            assertThat(a.isForAllClients()).isTrue();
        }
    }

    @Nested
    @DisplayName("Resolución")
    class Resolucion {

        @Test
        void resolver_marca_la_fecha() {
            Alert a = Alert.forAllClients(AlertType.DANGER, "ph-warning", "Crítico", null, null, null);
            LocalDateTime momento = LocalDateTime.of(2026, 9, 3, 14, 30);

            a.resolve(momento);

            assertThat(a.getResolved()).isTrue();
            assertThat(a.getResolvedAt()).isEqualTo(momento);
        }

        @Test
        void resolver_dos_veces_no_mueve_la_fecha_original() {
            // Idempotencia: dos clics en el botón no falsean cuándo se atendió.
            Alert a = Alert.forAllClients(AlertType.DANGER, "ph-warning", "Crítico", null, null, null);
            LocalDateTime primera = LocalDateTime.of(2026, 9, 3, 14, 30);

            a.resolve(primera);
            a.resolve(LocalDateTime.of(2026, 9, 3, 18, 0));

            assertThat(a.getResolvedAt()).isEqualTo(primera);
        }
    }

    @Nested
    @DisplayName("Valores por defecto y validación")
    class Defensas {

        @Test
        void una_alarma_sin_titulo_es_un_400() {
            assertThatThrownBy(() -> Alert.forAllClients(AlertType.INFO, null, "  ", null, null, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void sin_tipo_se_asume_informativa() {
            Alert a = Alert.forAllClients(null, null, "Aviso", null, null, null);
            assertThat(a.getType()).isEqualTo(AlertType.INFO);
            assertThat(a.isCritical()).isFalse();
        }

        @Test
        void sin_tiempo_relativo_se_pone_just_now() {
            Alert a = Alert.forAllClients(AlertType.INFO, null, "Aviso", null, "  ", null);
            assertThat(a.getRelativeTime()).isEqualTo("Just now");
        }

        @Test
        void nace_sin_resolver() {
            Alert a = Alert.forAllClients(AlertType.INFO, null, "Aviso", null, null, null);
            assertThat(a.getResolved()).isFalse();
            assertThat(a.getResolvedAt()).isNull();
        }

        @Test
        void solo_danger_cuenta_como_critica() {
            // Es lo que suma la tarjeta "alertas críticas" del panel.
            assertThat(Alert.forAllClients(AlertType.DANGER, null, "x", null, null, null).isCritical()).isTrue();
            assertThat(Alert.forAllClients(AlertType.WARNING, null, "x", null, null, null).isCritical()).isFalse();
            assertThat(Alert.forAllClients(AlertType.INFO, null, "x", null, null, null).isCritical()).isFalse();
        }
    }
}
