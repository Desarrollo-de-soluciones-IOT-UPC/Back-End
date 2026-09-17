package com.emsafe.assistant.application;

import com.emsafe.assistant.domain.model.Conversation;
import com.emsafe.assistant.domain.model.ConversationTurn;
import com.emsafe.assistant.domain.model.SensorContext;
import com.emsafe.assistant.domain.port.AssistantProvider;
import com.emsafe.assistant.domain.port.AssistantUnavailableException;
import com.emsafe.assistant.domain.service.PromptBuilder;
import com.emsafe.shared.domain.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Caso de uso del asistente: responder una pregunta fundamentada en los sensores que se
 * le pasen.
 *
 * <p>Nótese lo que este servicio <b>no</b> conoce: ni clientes, ni dispositivos, ni HTTP,
 * ni Gemini. Recibe el contexto ya traducido a {@link SensorContext} y habla con el
 * modelo por el puerto. El antiguo {@code AssistantService} hacía las cuatro cosas a la
 * vez — llamaba a {@code ClientService}, armaba el prompt, construía el JSON de Gemini y
 * parseaba la respuesta — en un solo archivo de 150 líneas.
 */
@Service
@RequiredArgsConstructor
public class AssistantApplicationService {

    private final AssistantProvider provider;
    private final PromptBuilder promptBuilder;

    /**
     * @param message  pregunta del usuario
     * @param history  turnos anteriores (el agregado se queda con los más recientes)
     * @param sensors  fotografía de los sensores con la que fundamentar la respuesta
     * @return el texto de la respuesta, listo para devolver
     */
    public String reply(String message, List<ConversationTurn> history, List<SensorContext> sensors) {
        if (!provider.isConfigured()) {
            throw new BadRequestException("The assistant is not configured on this server.");
        }

        // Las invariantes (mensaje obligatorio, tope de longitud, ventana de historial)
        // las aplica el agregado antes de que se gaste una llamada al proveedor.
        Conversation conversation = Conversation.of(message, history);

        String reply;
        try {
            reply = provider.reply(promptBuilder.build(sensors), conversation);
        } catch (AssistantUnavailableException e) {
            throw new BadRequestException("The assistant is unavailable right now. Try again in a moment.");
        }

        if (!StringUtils.hasText(reply)) {
            throw new BadRequestException("The assistant could not produce an answer. Try again.");
        }
        return reply.trim();
    }
}
