package com.emsafe.assistant.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root del contexto Assistant: la conversación que se envía al modelo.
 *
 * <p>Contiene las dos reglas que antes estaban sueltas dentro del service, mezcladas
 * con la construcción del JSON de la petición:
 * <ul>
 *   <li><b>La ventana de contexto:</b> solo viajan los últimos {@value #MAX_HISTORY_TURNS}
 *       turnos. Es una regla de coste y de calidad — el historial completo de una
 *       conversación larga quema cuota y diluye la respuesta.</li>
 *   <li><b>El límite del mensaje:</b> un mensaje vacío o desmedido se rechaza aquí, antes
 *       de gastar una llamada al proveedor.</li>
 * </ul>
 */
public final class Conversation {

    /** Turnos de historial que se conservan (los más recientes). */
    public static final int MAX_HISTORY_TURNS = 10;

    /** Tope del mensaje del usuario. Sin él, un cliente podía quemar la cuota de golpe. */
    public static final int MAX_MESSAGE_CHARS = 2000;

    private final List<ConversationTurn> turns;

    private Conversation(List<ConversationTurn> turns) {
        this.turns = List.copyOf(turns);
    }

    /**
     * Arma la conversación a enviar: el historial reciente más el mensaje nuevo.
     *
     * @param message mensaje del usuario; obligatorio
     * @param history turnos anteriores, del más antiguo al más reciente (puede ser null)
     */
    public static Conversation of(String message, List<ConversationTurn> history) {
        if (message == null || message.isBlank()) {
            throw new BadRequestException("message is required");
        }
        if (message.length() > MAX_MESSAGE_CHARS) {
            throw new BadRequestException(
                    "message is too long (max " + MAX_MESSAGE_CHARS + " characters)");
        }

        List<ConversationTurn> all = new ArrayList<>();
        if (history != null) {
            all.addAll(history.stream()
                    .skip(Math.max(0, history.size() - MAX_HISTORY_TURNS))
                    .toList());
        }
        all.add(ConversationTurn.fromUser(message.trim()));
        return new Conversation(all);
    }

    /** Turnos a enviar, en orden cronológico. */
    public List<ConversationTurn> turns() {
        return turns;
    }
}
