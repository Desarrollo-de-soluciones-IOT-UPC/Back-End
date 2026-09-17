package com.emsafe.clientportal.interfaces.rest.dto;

import com.emsafe.assistant.domain.model.Conversation;
import com.emsafe.assistant.domain.model.ConversationTurn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Una pregunta al chatbot desde la app móvil, con el historial opcional. */
public record ChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = Conversation.MAX_MESSAGE_CHARS,
              message = "message is too long")
        String message,
        List<ChatTurn> history
) {
    /** Un turno anterior de la conversación: role = "user" | "model". */
    public record ChatTurn(String role, String text) {}

    /** Traducción al vocabulario del contexto Assistant. */
    public List<ConversationTurn> toConversationTurns() {
        return history == null
                ? List.of()
                : history.stream().map(t -> ConversationTurn.of(t.role(), t.text())).toList();
    }
}
