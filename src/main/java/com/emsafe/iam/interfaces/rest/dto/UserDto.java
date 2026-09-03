package com.emsafe.iam.interfaces.rest.dto;

import com.emsafe.iam.domain.model.User;

import java.time.LocalDate;

public record UserDto(
        Long id,
        String name,
        String initials,
        String email,
        String role,
        String phone,
        String location,
        String status,
        String specialty,
        String department,
        LocalDate joinDate,
        String notes,
        String address,
        String clientType,
        String taxId,
        String industry,
        String country,
        String contactName,
        String contactEmail,
        String contactPhone
) {
    public static UserDto from(User u) {
        // El portal Angular espera el rol en Title Case ("Admin"/"Technician"/"Client")
        // y el status en minúscula: ambos formatos los define ahora el propio VO.
        String roleDisplay = u.getRole().displayValue();

        return new UserDto(
                u.getId(),
                u.getName(),
                u.getInitials(),
                u.getEmail(),
                roleDisplay,
                u.getPhone(),
                u.getLocation(),
                u.getStatus().persistedValue(),
                u.getSpecialty(),
                u.getDepartment(),
                u.getJoinDate(),
                u.getNotes(),
                u.getAddress(),
                u.getClientType(),
                u.getTaxId(),
                u.getIndustry(),
                u.getCountry(),
                u.getContactName(),
                u.getContactEmail(),
                u.getContactPhone()
        );
    }
}
