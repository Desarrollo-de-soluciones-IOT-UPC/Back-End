package com.emsafe.assistant.domain.port;

/**
 * El proveedor del asistente no pudo atender la petición (caída, timeout, cuota).
 *
 * <p>Es una excepción del puerto, no del proveedor concreto: el caso de uso reacciona a
 * "el asistente no está disponible" sin conocer {@code RestClientException} ni ningún
 * otro tipo de la infraestructura.
 */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
