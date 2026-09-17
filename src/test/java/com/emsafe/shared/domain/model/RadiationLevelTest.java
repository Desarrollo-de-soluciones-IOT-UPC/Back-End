package com.emsafe.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.emsafe.shared.domain.model.RadiationLevel.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * La regla "smart edge": el EDGE clasifica, el backend obedece.
 *
 * <p>Este VO está espejado en los otros tres stacks
 * ({@code value_objects.py}, {@code radiation-level.ts}, {@code radiation_level.dart}).
 * Si estos tests cambian, hay que cambiar los cuatro.
 *
 * <p>Existe porque ya falló una vez: la web clasificaba en µSv/h (0.10/0.30) mientras
 * el edge medía en µT (100/200), y <b>todas</b> las lecturas salían en rojo.
 */
class RadiationLevelTest {

    @Nested
    @DisplayName("El nivel del edge manda")
    class EdgeEsLaAutoridad {

        @Test
        void se_respeta_el_nivel_del_edge_aunque_contradiga_al_valor() {
            // 250 µT superaría el umbral de DANGER, pero el edge dijo SAFE.
            // El backend NO reclasifica: confía en quien mide.
            assertThat(of("safe", 250.0)).isEqualTo(SAFE);
            assertThat(of("danger", 5.0)).isEqualTo(DANGER);
        }

        @Test
        void el_nivel_del_edge_llega_en_mayusculas() {
            // El firmware envía "DANGER"; se guarda verbatim y debe parsearse igual.
            assertThat(of("DANGER", null)).isEqualTo(DANGER);
            assertThat(of("Caution", null)).isEqualTo(CAUTION);
            assertThat(of("  safe  ", null)).isEqualTo(SAFE);
        }
    }

    @Nested
    @DisplayName("Fallback por umbral (solo datos sin nivel)")
    class Fallback {

        @Test
        void sin_nivel_se_clasifica_por_el_valor() {
            assertThat(of(null, 45.0)).isEqualTo(SAFE);
            assertThat(of(null, 150.0)).isEqualTo(CAUTION);
            assertThat(of(null, 250.0)).isEqualTo(DANGER);
        }

        @Test
        void un_nivel_irreconocible_tambien_cae_al_valor() {
            assertThat(of("banana", 250.0)).isEqualTo(DANGER);
        }

        @Test
        void las_fronteras_de_los_umbrales() {
            assertThat(byValue(99.99)).isEqualTo(SAFE);
            assertThat(byValue(CAUTION_UT)).isEqualTo(CAUTION);     // 100 exacto → caution
            assertThat(byValue(199.99)).isEqualTo(CAUTION);
            assertThat(byValue(DANGER_UT)).isEqualTo(DANGER);       // 200 exacto → danger
        }

        @Test
        void los_umbrales_son_los_de_ICNIRP_50Hz() {
            // Cambiar estos números obliga a cambiar el edge, la web y el móvil.
            assertThat(CAUTION_UT).isEqualTo(100);
            assertThat(DANGER_UT).isEqualTo(200);
        }

        @Test
        void sin_nivel_y_sin_valor_se_asume_seguro() {
            assertThat(of(null, null)).isEqualTo(SAFE);
        }
    }

    @Nested
    @DisplayName("Contrato con front y móvil")
    class ContratoApi {

        @Test
        void los_literales_de_la_api_son_en_minuscula() {
            // Front y mobile dependen de estos strings exactos. No tocar.
            assertThat(SAFE.apiValue()).isEqualTo("safe");
            assertThat(CAUTION.apiValue()).isEqualTo("caution");
            assertThat(DANGER.apiValue()).isEqualTo("danger");
        }

        @Test
        void fromApi_degrada_a_safe_en_vez_de_reventar() {
            assertThat(fromApi("safe")).isEqualTo(SAFE);
            assertThat(fromApi(null)).isEqualTo(SAFE);
            assertThat(fromApi("???")).isEqualTo(SAFE);
        }
    }

    @Nested
    @DisplayName("Resumir varios sensores en un indicador")
    class PeorNivel {

        @Test
        void gana_el_mas_severo() {
            assertThat(SAFE.worseOf(DANGER)).isEqualTo(DANGER);
            assertThat(DANGER.worseOf(SAFE)).isEqualTo(DANGER);
            assertThat(CAUTION.worseOf(SAFE)).isEqualTo(CAUTION);
        }

        @Test
        void es_seguro_con_null() {
            assertThat(CAUTION.worseOf(null)).isEqualTo(CAUTION);
        }

        @Test
        void un_solo_sensor_en_peligro_tine_todo_el_panel() {
            // Así calcula el dashboard del cliente su nivel global.
            RadiationLevel global = java.util.stream.Stream
                    .of(SAFE, SAFE, DANGER, CAUTION)
                    .reduce(SAFE, RadiationLevel::worseOf);
            assertThat(global).isEqualTo(DANGER);
        }

        @Test
        void solo_danger_es_peligroso() {
            assertThat(DANGER.isDangerous()).isTrue();
            assertThat(CAUTION.isDangerous()).isFalse();
            assertThat(SAFE.isDangerous()).isFalse();
        }
    }
}
