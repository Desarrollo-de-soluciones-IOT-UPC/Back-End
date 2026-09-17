package com.emsafe.assistant.domain.port;

import com.emsafe.assistant.domain.model.Conversation;

/**
 * Puerto de SALIDA hacia el modelo de lenguaje que responde por Astra.
 *
 * <p>El dominio expresa lo que necesita — "dado este contexto y esta conversación,
 * dame una respuesta" — sin saber que detrás hay Gemini, HTTP, una clave de API ni un
 * formato de JSON con {@code candidates[0].content.parts[0].text}. Cambiar de proveedor
 * es escribir otro adaptador; el caso de uso no se toca.
 *
 * <p>Adaptador: {@code assistant/infrastructure/gemini/GeminiAssistantProvider}.
 */
public interface AssistantProvider {

    /** {@code false} si el servidor no tiene credenciales: el chat queda desactivado. */
    boolean isConfigured();

    /**
     * @return el texto de la respuesta, o {@code null} si el modelo no produjo ninguna
     * @throws com.emsafe.assistant.domain.port.AssistantUnavailableException si el
     *         proveedor falla o no responde a tiempo
     */
    String reply(String systemPrompt, Conversation conversation);
}
