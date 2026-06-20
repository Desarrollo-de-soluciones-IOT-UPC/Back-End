package com.emsafe.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time telemetry over STOMP + in-memory SimpleBroker (no external broker).
 *
 * The edge keeps POSTing readings over HTTP ({@code /api/v1/readings}); after a
 * reading is saved, {@code TelemetryService} broadcasts it to:
 *   - {@code /topic/readings}  → every new reading (radiation map, dashboard, mobile).
 *   - {@code /topic/discovery} → updates of "unregistered" sensors (technician's
 *                                installation discovery panel).
 *
 * Same contract is consumed by web (Angular) and mobile (Flutter).
 *
 * Caveat: SimpleBroker fans out within a single instance only (Azure App Service
 * single-instance is fine). Multi-instance would need Redis/Azure SignalR.
 * Remember to enable "Web Sockets" in the Azure App Service configuration.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback (web) + raw WebSocket (mobile). Origins open like /api/v1.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
