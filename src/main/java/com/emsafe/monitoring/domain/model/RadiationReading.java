package com.emsafe.monitoring.domain.model;

import com.emsafe.device.domain.model.Device;
import com.emsafe.iam.domain.model.User;
import com.emsafe.shared.domain.exception.BadRequestException;
import com.emsafe.shared.domain.model.RadiationLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate Root del bounded context Monitoring: una medición de campo
 * electromagnético reportada por el edge.
 *
 * <p>Espeja la entidad {@code EmfReading} del bounded context {@code monitoring} del
 * edge Flask — mismo nombre de contexto a ambos lados de la frontera, que es lo que
 * pide el lenguaje ubicuo.
 *
 * <p><b>Smart edge:</b> el nivel lo calcula el EDGE y aquí solo se conserva. El
 * backend jamás reclasifica; {@link #effectiveLevel()} usa el nivel reportado y solo
 * recurre a los umbrales si la lectura llegó sin nivel (semilla o datos legacy).
 *
 * <p><b>Nota de contrato (R1):</b> {@code level} se guarda como String, tal cual lo
 * manda el edge (en MAYÚSCULA: "DANGER"). Convertirlo a enum con converter
 * normalizaría el valor y cambiaría el JSON que ya consumen el front y la app móvil.
 * El VO {@link RadiationLevel} se usa para razonar sobre el nivel, no para persistirlo.
 */
@Entity
@Table(name = "radiation_readings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RadiationReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate readingDate;

    /** Magnitud del campo en microtesla (µT). */
    @Column(nullable = false)
    private Double value;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 200)
    private String location;

    /** Serial del sensor que la reportó (= Device.serialNumber). */
    @Column(length = 50)
    private String sensorId;

    /** Nivel reportado por el edge (SAFE | CAUTION | DANGER), verbatim. */
    @Column(length = 30)
    private String level;

    /** Mensaje descriptivo que acompaña la lectura. */
    @Column(length = 255)
    private String message;

    /** Estado del relé reportado por el dispositivo en esta lectura (ON | OFF). */
    @Column(length = 8)
    private String plug;

    /** Timestamp real de la medición (el edge ingesta varias por día). */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    // ─── Factoría de dominio ──────────────────────────────────────────────────

    /**
     * Construye una lectura a partir de lo que reporta el edge, heredando la
     * ubicación del sensor y las coordenadas del cliente dueño.
     *
     * @throws BadRequestException si la magnitud falta o es negativa
     */
    public static RadiationReading fromEdge(String serialNumber, Double fieldUT, String reportedLevel,
                                            String message, String plug, Device device, LocalDateTime when) {
        if (fieldUT == null) {
            throw new BadRequestException("field_uT is required");
        }
        if (fieldUT < 0) {
            throw new BadRequestException("field_uT cannot be negative");
        }
        LocalDateTime timestamp = when != null ? when : LocalDateTime.now();
        User owner = device != null ? device.getClient() : null;

        return RadiationReading.builder()
                .readingDate(timestamp.toLocalDate())
                .recordedAt(timestamp)
                .value(fieldUT)
                .level(reportedLevel)
                .message(message)
                .plug(plug)
                .sensorId(serialNumber)
                .location(device != null ? device.getLocation() : null)
                .latitude(owner != null ? owner.getLatitude() : null)
                .longitude(owner != null ? owner.getLongitude() : null)
                .device(device)
                .build();
    }

    // ─── Comportamiento de negocio ────────────────────────────────────────────

    /**
     * Nivel efectivo: se respeta el que calculó el edge y solo se clasifica por
     * umbral cuando la lectura no trae nivel válido. Esta es la única puerta por
     * la que el resto del sistema debe preguntar el nivel de una lectura.
     */
    public RadiationLevel effectiveLevel() {
        return RadiationLevel.of(level, value);
    }

    /** Literal en minúscula que espera la API ("safe" | "caution" | "danger"). */
    public String levelForApi() {
        return effectiveLevel().apiValue();
    }

    public boolean isDangerous() {
        return effectiveLevel().isDangerous();
    }

    /** Cliente dueño del sensor, si el sensor ya fue reclamado. */
    public User owner() {
        return device != null ? device.getClient() : null;
    }

    /** Momento de la medición, con la fecha como respaldo para datos antiguos. */
    public LocalDateTime timestamp() {
        return recordedAt != null ? recordedAt : readingDate.atStartOfDay();
    }

    /** Reescalado de la semilla a µT — uso exclusivo del DataInitializer. */
    public void rescaleSeed(double newValue, String newLevel, LocalDateTime newRecordedAt) {
        this.value = newValue;
        this.level = newLevel;
        this.recordedAt = newRecordedAt;
    }
}
