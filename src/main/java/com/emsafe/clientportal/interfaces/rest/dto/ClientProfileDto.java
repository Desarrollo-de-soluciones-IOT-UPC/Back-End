package com.emsafe.clientportal.interfaces.rest.dto;

import com.emsafe.iam.domain.model.User;

import java.time.LocalDate;

/**
 * Perfil del cliente autenticado (app móvil).
 * El rol va en minúscula ("client") para ser coherente con la respuesta del login.
 */
public record ClientProfileDto(
        Long id,
        String name,
        String initials,
        String email,
        String role,
        String phone,
        String location,
        String address,
        Double latitude,
        Double longitude,
        LocalDate joinDate
) {
    public static ClientProfileDto from(User u) {
        return new ClientProfileDto(
                u.getId(),
                u.getName(),
                u.getInitials(),
                u.getEmail(),
                u.getRole().name().toLowerCase(),
                u.getPhone(),
                u.getLocation(),
                u.getAddress(),
                u.getLatitude(),
                u.getLongitude(),
                u.getJoinDate()
        );
    }
}
