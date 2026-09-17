package com.emsafe.clientportal.interfaces.rest.dto;

/**
 * Campos que un cliente puede cambiar de su propio perfil desde la app móvil.
 * Email, rol y contraseña quedan fuera a propósito.
 */
public record UpdateClientProfileRequest(
        String name,
        String phone,
        String location,
        String address,
        Double latitude,
        Double longitude
) {}
