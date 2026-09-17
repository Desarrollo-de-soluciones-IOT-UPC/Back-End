package com.emsafe.clientportal.application;

import com.emsafe.assistant.application.AssistantApplicationService;
import com.emsafe.assistant.domain.model.SensorContext;
import com.emsafe.clientportal.interfaces.rest.dto.ChatReplyDto;
import com.emsafe.clientportal.interfaces.rest.dto.ChatRequest;
import com.emsafe.clientportal.interfaces.rest.dto.ClientDeviceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Une las dos mitades del chat de Astra: los sensores del cliente y el asistente.
 *
 * <p>Aquí está la <b>inversión de dependencia</b> que trae la fase 8. Antes,
 * {@code AssistantService} llamaba a {@code ClientService.getDevices()}: el asistente
 * dependía del portal móvil y de la forma de su DTO. Ahora es al revés — el portal, que
 * es quien sabe qué sensores tiene el cliente, se los traduce al vocabulario de Assistant
 * ({@link SensorContext}) y se los entrega. Assistant no conoce clientes ni dispositivos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientAssistantService {

    private final ClientDeviceService clientDevices;
    private final AssistantApplicationService assistant;

    public ChatReplyDto chat(Long clientId, ChatRequest req) {
        return new ChatReplyDto(
                assistant.reply(req.message(), req.toConversationTurns(), sensorsOf(clientId)));
    }

    /**
     * Fotografía de los sensores del cliente. Es "mejor esfuerzo": si falla, Astra
     * responde igual, solo que sin datos concretos del usuario.
     */
    private List<SensorContext> sensorsOf(Long clientId) {
        try {
            return clientDevices.getDevices(clientId).stream()
                    .map(ClientDeviceDto::toSensorContext)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
