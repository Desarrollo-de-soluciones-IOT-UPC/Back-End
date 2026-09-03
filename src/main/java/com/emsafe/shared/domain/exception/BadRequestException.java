package com.emsafe.shared.domain.exception;

/**
 * Invariante de negocio violada por datos de entrada inválidos → HTTP 400.
 */
public class BadRequestException extends DomainException {
    public BadRequestException(String message) {
        super(message);
    }
}
