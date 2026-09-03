package com.emsafe.alerting.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root del bounded context Alerting: un aviso mostrado en el portal.
 *
 * <p>Una alarma se dirige o bien a TODOS los clientes, o bien a un subconjunto
 * concreto. Esa es su invariante principal y antes se mantenía a mano en
 * {@code AlarmApplicationService} (poner {@code recipientType}, rellenar la lista de ids y
 * denormalizar el nombre eran tres pasos separados que había que recordar).
 * Ahora las factorías {@link #forAllClients} y {@link #forClients} lo garantizan.
 *
 * <p><b>Persistencia sin cambios (R2/R3):</b> misma tabla {@code alerts} y misma
 * tabla de colección {@code alert_recipient_clients}.
 */
@Entity
@Table(name = "alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** danger | info | success | warning */
    @Column(nullable = false, length = 20)
    private AlertType type;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String relativeTime;

    /** all | specific — a quién va dirigida la alarma. */
    @Column(name = "recipient_type", length = 30)
    private String recipientType;

    /** Nombre(s) de cliente denormalizados para el detalle de la alarma. */
    @Column(name = "client_name", length = 300)
    private String clientName;

    /** Sensor al que hace referencia la alarma (vista de detalle). */
    @Column(length = 100)
    private String sensor;

    /** Destinatarios concretos. Vacío cuando recipientType = all. */
    @ElementCollection
    @CollectionTable(name = "alert_recipient_clients",
            joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "client_id")
    @Builder.Default
    private List<Long> recipientClientIds = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column
    private LocalDateTime resolvedAt;

    private static final String RECIPIENT_ALL = "all";
    private static final String RECIPIENT_SPECIFIC = "specific";

    // ─── Factorías de dominio ─────────────────────────────────────────────────

    /** Alarma general: la ven todos los clientes. */
    public static Alert forAllClients(AlertType type, String icon, String title,
                                      String description, String relativeTime, String sensor) {
        return baseBuilder(type, icon, title, description, relativeTime, sensor)
                .recipientType(RECIPIENT_ALL)
                .clientName("All clients")
                .build();
    }

    /**
     * Alarma dirigida a clientes concretos. Mantiene coherentes los tres campos que
     * antes se seteaban por separado: tipo de destinatario, ids y nombres visibles.
     */
    public static Alert forClients(AlertType type, String icon, String title,
                                   String description, String relativeTime, String sensor,
                                   List<Long> clientIds, String clientNames) {
        if (clientIds == null || clientIds.isEmpty()) {
            // Sin destinatarios concretos, "specific" no significaría nada.
            return forAllClients(type, icon, title, description, relativeTime, sensor);
        }
        Alert alert = baseBuilder(type, icon, title, description, relativeTime, sensor)
                .recipientType(RECIPIENT_SPECIFIC)
                .clientName(clientNames)
                .build();
        alert.recipientClientIds.addAll(clientIds);
        return alert;
    }

    private static AlertBuilder baseBuilder(AlertType type, String icon, String title,
                                            String description, String relativeTime, String sensor) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Alarm title is required");
        }
        return Alert.builder()
                .type(type != null ? type : AlertType.INFO)
                .icon(icon)
                .title(title)
                .description(description)
                .relativeTime(relativeTime != null && !relativeTime.isBlank() ? relativeTime : "Just now")
                .sensor(sensor)
                .createdAt(LocalDateTime.now())
                .resolved(false);
    }

    // ─── Comportamiento de negocio ────────────────────────────────────────────

    /** Marca la alarma como atendida. Idempotente: re-resolver no mueve la fecha. */
    public void resolve(LocalDateTime when) {
        if (Boolean.TRUE.equals(resolved)) {
            return;
        }
        this.resolved = true;
        this.resolvedAt = when != null ? when : LocalDateTime.now();
    }

    public boolean isCritical() {
        return type != null && type.isCritical();
    }

    public boolean isForAllClients() {
        return RECIPIENT_ALL.equalsIgnoreCase(recipientType);
    }

    /** ¿Debe ver esta alarma el cliente indicado? */
    public boolean isVisibleTo(Long clientId) {
        return isForAllClients() || (clientId != null && recipientClientIds.contains(clientId));
    }
}
