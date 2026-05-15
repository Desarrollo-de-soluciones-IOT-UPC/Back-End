package com.emsafe.user.dto;

import com.emsafe.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        String name,

        String initials,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        String phone,
        String location,
        String specialty,
        String department,
        LocalDate joinDate
) {}
