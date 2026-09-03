package com.emsafe.shared.domain.exception;

/**
 * El agregado solicitado no existe → HTTP 404.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
}
