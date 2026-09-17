package com.emsafe.assistant.domain.model;

/**
 * Value Object: un turno de la conversación con Astra.
 *
 * <p>El rol es un enum y no un {@code String} porque solo hay dos hablantes posibles y
 * el proveedor rechaza cualquier otra cosa. Antes, la normalización
 * ({@code "model".equalsIgnoreCase(...) ? "model" : "user"}) estaba escrita a mano
 * dentro del bucle que armaba el cuerpo de la petición HTTP.
 */
public record ConversationTurn(Speaker speaker, String text) {

    public enum Speaker {
        USER("user"),
        MODEL("model");

        private final String wireValue;

        Speaker(String wireValue) {
            this.wireValue = wireValue;
        }

        /** Literal que espera el proveedor. */
        public String wireValue() {
            return wireValue;
        }

        /** Cualquier cosa que no sea "model" se considera del usuario. */
        public static Speaker fromApi(String value) {
            return "model".equalsIgnoreCase(value) ? MODEL : USER;
        }
    }

    public static ConversationTurn of(String role, String text) {
        return new ConversationTurn(Speaker.fromApi(role), text == null ? "" : text);
    }

    public static ConversationTurn fromUser(String text) {
        return new ConversationTurn(Speaker.USER, text);
    }
}
