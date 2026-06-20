package com.emsafe.dashboard.entity;

import com.emsafe.device.entity.Device;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 200)
    private String location;

    @Column(length = 50)
    private String sensorId;

    /** Nivel reportado por el edge (e.g. SEGURO | MODERADO | ALTO). */
    @Column(length = 30)
    private String level;

    /** Mensaje descriptivo que acompaña la lectura. */
    @Column(length = 255)
    private String message;

    /** Timestamp real de la medición (el edge ingesta varias por día). */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;
}
