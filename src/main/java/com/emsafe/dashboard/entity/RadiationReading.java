package com.emsafe.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "radiation_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadiationReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate readingDate;

    @Column(nullable = false)
    private Double value;
}
