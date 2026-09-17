package com.emsafe.assistant.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El agregado {@link Conversation}: las dos reglas que protegen la cuota de Gemini.
 *
 * <p>Estaban sueltas dentro del código que armaba el JSON de la petición HTTP. Ahora
 * se aplican <b>antes</b> de gastar una llamada al proveedor.
 */
class ConversationTest {

    private static List<ConversationTurn> historial(int turnos) {
        List<ConversationTurn> list = new ArrayList<>();
        for (int i = 0; i < turnos; i++) {
            list.add(ConversationTurn.of(i % 2 == 0 ? "user" : "model", "mensaje " + i));
        }
        return list;
    }

    @Nested
    @DisplayName("Ventana de contexto")
    class VentanaDeContexto {

        @Test
        void solo_viajan_los_ultimos_turnos_mas_el_mensaje_nuevo() {
            // Regla de coste y de calidad: el historial completo de una conversación
            // larga quema cuota y diluye la respuesta.
            Conversation c = Conversation.of("¿estoy seguro?", historial(30));

            assertThat(c.turns()).hasSize(Conversation.MAX_HISTORY_TURNS + 1);
        }

        @Test
        void se_conservan_los_mas_recientes_no_los_primeros() {
            Conversation c = Conversation.of("nuevo", historial(30));

            assertThat(c.turns().get(0).text()).isEqualTo("mensaje 20");
            assertThat(c.turns().get(c.turns().size() - 1).text()).isEqualTo("nuevo");
        }

        @Test
        void una_conversacion_corta_viaja_entera() {
            Conversation c = Conversation.of("hola", historial(3));
            assertThat(c.turns()).hasSize(4);
        }

        @Test
        void sin_historial_solo_va_el_mensaje() {
            Conversation c = Conversation.of("hola", null);

            assertThat(c.turns()).hasSize(1);
            assertThat(c.turns().get(0).speaker()).isEqualTo(ConversationTurn.Speaker.USER);
        }

        @Test
        void el_mensaje_nuevo_siempre_va_al_final_y_es_del_usuario() {
            Conversation c = Conversation.of("  ¿y ahora?  ", historial(5));
            ConversationTurn ultimo = c.turns().get(c.turns().size() - 1);

            assertThat(ultimo.speaker()).isEqualTo(ConversationTurn.Speaker.USER);
            assertThat(ultimo.text()).isEqualTo("¿y ahora?");   // recortado
        }
    }

    @Nested
    @DisplayName("Validación del mensaje")
    class Validacion {

        @Test
        void un_mensaje_vacio_no_gasta_una_llamada() {
            assertThatThrownBy(() -> Conversation.of("", null))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> Conversation.of("   ", null))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> Conversation.of(null, null))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void un_mensaje_desmedido_se_rechaza_antes_de_llamar_a_gemini() {
            // Sin este tope, un cliente podía quemar la cuota de golpe (hallazgo N6).
            String enorme = "a".repeat(Conversation.MAX_MESSAGE_CHARS + 1);

            assertThatThrownBy(() -> Conversation.of(enorme, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("too long");
        }

        @Test
        void justo_en_el_limite_se_acepta() {
            String enElLimite = "a".repeat(Conversation.MAX_MESSAGE_CHARS);
            assertThat(Conversation.of(enElLimite, null).turns()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Turnos")
    class Turnos {

        @Test
        void cualquier_rol_que_no_sea_model_es_del_usuario() {
            // Normalización que antes estaba escrita a mano dentro del bucle
            // que armaba el cuerpo de la petición.
            assertThat(ConversationTurn.of("model", "x").speaker())
                    .isEqualTo(ConversationTurn.Speaker.MODEL);
            assertThat(ConversationTurn.of("MODEL", "x").speaker())
                    .isEqualTo(ConversationTurn.Speaker.MODEL);
            assertThat(ConversationTurn.of("user", "x").speaker())
                    .isEqualTo(ConversationTurn.Speaker.USER);
            assertThat(ConversationTurn.of("cualquier-cosa", "x").speaker())
                    .isEqualTo(ConversationTurn.Speaker.USER);
            assertThat(ConversationTurn.of(null, "x").speaker())
                    .isEqualTo(ConversationTurn.Speaker.USER);
        }

        @Test
        void el_literal_del_rol_es_el_que_espera_el_proveedor() {
            assertThat(ConversationTurn.Speaker.USER.wireValue()).isEqualTo("user");
            assertThat(ConversationTurn.Speaker.MODEL.wireValue()).isEqualTo("model");
        }

        @Test
        void un_texto_nulo_no_revienta() {
            assertThat(ConversationTurn.of("user", null).text()).isEmpty();
        }
    }
}
