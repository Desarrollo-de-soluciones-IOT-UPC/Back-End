package com.emsafe.assistant.infrastructure.gemini;

import com.emsafe.assistant.domain.model.Conversation;
import com.emsafe.assistant.domain.model.ConversationTurn;
import com.emsafe.assistant.domain.port.AssistantProvider;
import com.emsafe.assistant.domain.port.AssistantUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Adaptador del puerto {@link AssistantProvider} sobre la API de Gemini.
 *
 * <p>Todo lo que es "Gemini" vive aquí y solo aquí: la URL, el nombre del modelo, la
 * forma del JSON ({@code system_instruction} / {@code contents} / {@code candidates})
 * y la credencial.
 *
 * <p><b>Dos hallazgos de la auditoría corregidos en este archivo:</b>
 * <ol>
 *   <li><b>N1 — {@code RestTemplate} sin timeouts.</b> El anterior se construía con
 *       {@code new RestTemplate()}, que <i>no tiene timeout por defecto</i>: si Gemini
 *       tardaba o no cerraba la conexión, el hilo de Tomcat quedaba bloqueado
 *       indefinidamente. Con suficientes peticiones así se agota el pool y **cae toda la
 *       API**, no solo el chat. Ahora hay timeout de conexión y de lectura, ambos
 *       configurables.</li>
 *   <li><b>N2 — la clave viajaba en la query string</b> ({@code ?key=...}), donde acaba
 *       en logs de acceso, proxies e historiales. Ahora va en la cabecera
 *       {@code x-goog-api-key}. Y como el mensaje de excepción de {@code RestTemplate}
 *       incluye la URL, se dejó de loguear {@code e.getMessage()} en crudo: se registra
 *       el tipo de fallo, que es lo que sirve para diagnosticar.</li>
 * </ol>
 */
@Slf4j
@Component
public class GeminiAssistantProvider implements AssistantProvider {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public GeminiAssistantProvider(
            RestTemplateBuilder builder,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            @Value("${gemini.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${gemini.read-timeout-ms:20000}") long readTimeoutMs) {
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String reply(String systemPrompt, Conversation conversation) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // N2: la credencial va en cabecera, nunca en la URL.
        headers.set("x-goog-api-key", apiKey);

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", toContents(conversation),
                "generationConfig", Map.of(
                        "temperature", 0.6,
                        "maxOutputTokens", 700
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    String.format(GEMINI_URL, model), new HttpEntity<>(body, headers), Map.class);
            return extractText(response);
        } catch (Exception e) {
            // Sin e.getMessage(): el mensaje de RestTemplate arrastra la URL de la
            // petición y no queremos ese detalle en los logs del servidor.
            log.warn("Gemini call failed: {}", e.getClass().getSimpleName());
            throw new AssistantUnavailableException("Gemini call failed", e);
        }
    }

    private List<Map<String, Object>> toContents(Conversation conversation) {
        return conversation.turns().stream()
                .map(turn -> Map.<String, Object>of(
                        "role", turn.speaker().wireValue(),
                        "parts", List.of(Map.of("text", turn.text()))))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) return null;
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return null;
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return null;
        Object text = parts.get(0).get("text");
        return text != null ? text.toString() : null;
    }
}
