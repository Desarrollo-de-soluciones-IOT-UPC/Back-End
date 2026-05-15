package com.emsafe.auth.dto;

public record LoginResponse(
        String token,
        String email,
        String name,
        String initials,
        String role,
        Long userId,
        Long technicianId
) {}
