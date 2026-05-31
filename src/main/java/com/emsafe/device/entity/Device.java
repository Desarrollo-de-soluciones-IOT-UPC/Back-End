package com.emsafe.device.entity;

import com.emsafe.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 200)
    private String location;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "active";

    @Column(length = 100)
    private String serialNumber;

    @Column
    private LocalDate installDate;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private AppUser client;
}
