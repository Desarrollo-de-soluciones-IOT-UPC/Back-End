package com.emsafe.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 5)
    private String initials;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String location;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(length = 100)
    private String specialty;

    @Column(length = 100)
    private String department;

    private LocalDate joinDate;

    private LocalDateTime lastLogin;
}
