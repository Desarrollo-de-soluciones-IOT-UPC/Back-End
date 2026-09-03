package com.emsafe.shared.domain.exception;

/**
 * Raíz de las excepciones de dominio: se lanza cuando se viola una invariante
 * de negocio (no un fallo técnico).
 *
 * <p>Vive en el Shared Kernel para que cualquier agregado pueda defender sus
 * invariantes sin depender de Spring ni de la capa web. La traducción a códigos
 * HTTP es responsabilidad de {@code interfaces/rest/GlobalExceptionHandler}.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
