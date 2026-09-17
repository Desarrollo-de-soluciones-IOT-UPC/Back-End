package com.emsafe.clientportal.interfaces.rest;

import com.emsafe.iam.infrastructure.security.JwtUtil;
import com.emsafe.shared.domain.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resuelve QUIÉN hace la petición en el portal del cliente.
 *
 * <p>Cada endpoint de {@code /api/client/**} está acotado al dueño del JWT. Al partir el
 * controller monolítico en seis, ese {@code extractUserId} privado se habría copiado seis
 * veces; aquí vive una sola vez.
 *
 * <p>Un token sin id de usuario es un token malformado, así que se rechaza con 400 en
 * lugar de dejar que el id nulo se filtre hasta las consultas.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedClient {

    private final JwtUtil jwtUtil;

    public Long id(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        Long userId = StringUtils.hasText(header) && header.startsWith("Bearer ")
                ? jwtUtil.extractUserId(header.substring(7))
                : null;
        if (userId == null) {
            throw new BadRequestException("Missing user id in token");
        }
        return userId;
    }
}
