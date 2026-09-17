package com.emsafe.workorder.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.emsafe.workorder.domain.model.WorkOrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La máquina de estados de una orden de trabajo.
 *
 * <pre>
 *   PENDING ──▶ IN_PROGRESS ──▶ COMPLETED
 *      │              │
 *      └──────────────┴──────▶ CANCELLED
 * </pre>
 *
 * <p>Esta regla vivía en un método privado de {@code WorkOrderService} (512 líneas) y
 * no había forma de probarla sin levantar Spring entero. Ahora es un VO puro.
 */
class WorkOrderStatusTest {

    @Nested
    @DisplayName("Transiciones válidas")
    class Validas {

        @Test
        void una_orden_pendiente_puede_arrancar_o_cancelarse() {
            assertThat(PENDING.canTransitionTo(IN_PROGRESS)).isTrue();
            assertThat(PENDING.canTransitionTo(CANCELLED)).isTrue();
        }

        @Test
        void una_orden_en_curso_puede_completarse_o_cancelarse() {
            assertThat(IN_PROGRESS.canTransitionTo(COMPLETED)).isTrue();
            assertThat(IN_PROGRESS.canTransitionTo(CANCELLED)).isTrue();
        }

        @Test
        void reenviar_el_mismo_estado_es_idempotente() {
            for (WorkOrderStatus status : values()) {
                assertThat(status.canTransitionTo(status))
                        .as("%s → %s debe permitirse (el parte del técnico reenvía el estado actual)",
                                status, status)
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Transiciones prohibidas")
    class Invalidas {

        @Test
        void no_se_puede_completar_una_orden_que_nunca_arrancó() {
            assertThat(PENDING.canTransitionTo(COMPLETED)).isFalse();
        }

        @Test
        void una_orden_completada_es_terminal() {
            assertThat(COMPLETED.canTransitionTo(IN_PROGRESS)).isFalse();
            assertThat(COMPLETED.canTransitionTo(PENDING)).isFalse();
            assertThat(COMPLETED.canTransitionTo(CANCELLED)).isFalse();
        }

        @Test
        void una_orden_cancelada_es_terminal() {
            assertThat(CANCELLED.canTransitionTo(IN_PROGRESS)).isFalse();
            assertThat(CANCELLED.canTransitionTo(PENDING)).isFalse();
            assertThat(CANCELLED.canTransitionTo(COMPLETED)).isFalse();
        }

        @Test
        void no_se_puede_retroceder_de_en_curso_a_pendiente() {
            assertThat(IN_PROGRESS.canTransitionTo(PENDING)).isFalse();
        }
    }

    @Nested
    @DisplayName("Clasificación")
    class Clasificacion {

        @Test
        void solo_pendiente_y_en_curso_estan_activas() {
            assertThat(PENDING.isActive()).isTrue();
            assertThat(IN_PROGRESS.isActive()).isTrue();
            assertThat(COMPLETED.isActive()).isFalse();
            assertThat(CANCELLED.isActive()).isFalse();
        }

        @Test
        void activa_y_terminal_son_complementarias() {
            for (WorkOrderStatus status : values()) {
                assertThat(status.isActive()).isNotEqualTo(status.isTerminal());
            }
        }
    }

    @Nested
    @DisplayName("Parseo desde la API")
    class Parseo {

        @Test
        void acepta_el_formato_con_guion_del_front() {
            assertThat(fromApi("in-progress")).isEqualTo(IN_PROGRESS);
        }

        @Test
        void acepta_el_nombre_del_enum_y_no_distingue_mayusculas() {
            assertThat(fromApi("IN_PROGRESS")).isEqualTo(IN_PROGRESS);
            assertThat(fromApi("Completed")).isEqualTo(COMPLETED);
            assertThat(fromApi("  pending  ")).isEqualTo(PENDING);
        }

        @Test
        void un_estado_inventado_es_un_400() {
            assertThatThrownBy(() -> fromApi("teleported"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("teleported");
        }

        @Test
        void un_estado_vacio_es_un_400() {
            assertThatThrownBy(() -> fromApi("  ")).isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> fromApi(null)).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Parseo de filtros de listado")
    class Filtros {

        @Test
        void sin_filtro_devuelve_null_en_vez_de_fallar() {
            assertThat(parseFilter(null)).isNull();
            assertThat(parseFilter("")).isNull();
            assertThat(parseFilter("   ")).isNull();
        }

        @Test
        void la_palabra_all_significa_sin_filtro() {
            assertThat(parseFilter("all")).isNull();
            assertThat(parseFilter("ALL")).isNull();
        }

        @Test
        void un_filtro_valido_si_se_parsea() {
            assertThat(parseFilter("pending")).isEqualTo(PENDING);
        }

        @Test
        void un_filtro_inventado_sigue_siendo_un_400() {
            assertThatThrownBy(() -> parseFilter("noexiste"))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
