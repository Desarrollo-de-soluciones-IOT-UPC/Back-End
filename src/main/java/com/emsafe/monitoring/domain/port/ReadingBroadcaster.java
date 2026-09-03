package com.emsafe.monitoring.domain.port;

import com.emsafe.monitoring.interfaces.rest.dto.ReadingDto;

/**
 * Puerto de salida: difusión en tiempo real de una lectura recién ingerida.
 *
 * <p>Antes, {@code TelemetryService} inyectaba directamente el
 * {@code SimpMessagingTemplate} de Spring y sabía los nombres de los topics STOMP.
 * Ahora la capa de aplicación solo expresa la intención ("difunde esta lectura") y
 * el adaptador {@code infrastructure/realtime/StompReadingBroadcaster} decide cómo.
 * Cambiar STOMP por SSE o por un broker externo no tocaría el caso de uso.
 */
public interface ReadingBroadcaster {

    /**
     * Difunde la lectura a quien corresponda: el topic global (web admin/técnico),
     * el topic privado del cliente dueño, y el de descubrimiento si el sensor
     * todavía no está reclamado.
     *
     * @param clientId cliente dueño del sensor, o null si aún no tiene
     * @param unregistered true si el sensor sigue en el pool sin asignar
     */
    void broadcast(ReadingDto reading, Long clientId, boolean unregistered);
}
