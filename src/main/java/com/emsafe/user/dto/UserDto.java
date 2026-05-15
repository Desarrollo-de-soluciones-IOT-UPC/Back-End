package com.emsafe.user.dto;

import com.emsafe.user.entity.AppUser;

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
        LocalDate joinDate
) {
    public static UserDto from(AppUser u) {
        // Serialize role as title-case to match Angular frontend ("Admin", "Technician", "Client")
        String roleName = u.getRole().name();
        String roleDisplay = roleName.charAt(0) + roleName.substring(1).toLowerCase();

        return new UserDto(
                u.getId(),
                u.getName(),
                u.getInitials(),
                u.getEmail(),
                roleDisplay,
                u.getPhone(),
                u.getLocation(),
                u.getStatus(),
                u.getSpecialty(),
                u.getDepartment(),
                u.getJoinDate()
        );
    }
}
