package com.emsafe.monitoring.infrastructure.realtime;

import com.emsafe.monitoring.domain.port.ReadingBroadcaster;
import com.emsafe.monitoring.interfaces.rest.dto.ReadingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador STOMP del puerto {@link ReadingBroadcaster}.
 *
 * <p>Es el único sitio que conoce los nombres de los topics. El reparto tiene
 * una razón de privacidad: un cliente solo se suscribe a SU topic, así que nunca
 * recibe —ni le saltan alertas DANGER de— sensores de otros clientes.
 */
@Component
@RequiredArgsConstructor
public class StompReadingBroadcaster implements ReadingBroadcaster {

    /** Topic global: web de admin y técnico (mapa de radiación, dashboard). */
    private static final String TOPIC_ALL = "/topic/readings";
    /** Topic del panel de instalación: sensores todavía sin dueño. */
    private static final String TOPIC_DISCOVERY = "/topic/discovery";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(ReadingDto reading, Long clientId, boolean unregistered) {
        messagingTemplate.convertAndSend(TOPIC_ALL, reading);

        if (clientId != null) {
            messagingTemplate.convertAndSend("/topic/clients/" + clientId + "/readings", reading);
        }
        if (unregistered) {
            messagingTemplate.convertAndSend(TOPIC_DISCOVERY, reading);
        }
    }
}
