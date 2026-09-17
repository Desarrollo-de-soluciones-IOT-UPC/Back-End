package com.emsafe.servicerecord.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Aggregate Root del bounded context ServiceRecord: el <b>acta de servicio</b> que
 * queda cuando una orden de trabajo se cierra.
 *
 * <p>Antes se llamaba {@code History} — un nombre técnico, no del negocio — y era una
 * bolsa de datos con {@code @Setter} que construía {@code WorkOrderService} a mano.
 *
 * <p><b>Es un snapshot, no una vista.</b> Copia el cliente, la sede, el tipo de
 * servicio y el técnico tal como estaban <i>en el momento del cierre</i>. Si mañana
 * la orden se reasigna o el cliente cambia de nombre, el acta no se altera: eso es
 * justamente lo que la hace un registro histórico y no un JOIN. Por eso solo tiene
 * factorías y ningún método de mutación — un acta emitida es inmutable.
 *
 * <p><b>Persistencia sin cambios (R2/R3):</b> misma tabla {@code history} y mismas
 * columnas de siempre; {@code status} se mapea con un converter de aplicación
 * automática que guarda los literales "completed"/"cancelled". Sin Flyway.
 */
@Entity
@Table(name = "history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ServiceRecord {

    /**
     * Formato de la hora de cierre que espera el historial del portal.
     * {@code Locale.ENGLISH} fija el AM/PM: con el locale del servidor en español,
     * {@code hh:mm a} produce "a. m." y rompería el formato de la columna.
     */
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código legible de la orden que originó el acta (#WO-0001). */
    @Column(length = 30)
    private String orderId;

    private LocalDate completionDate;

    @Column(length = 20)
    private String completionTime;

    @Column(length = 150)
    private String client;

    @Column(length = 200)
    private String site;

    @Column(length = 20)
    private String serviceType;

    @Column(length = 100)
    private String technician;

    @Column(length = 5)
    private String technicianInitials;

    @Column(nullable = false, length = 20)
    private ServiceRecordStatus status;

    /** Técnico que firmó el acta — filtra el historial del portal técnico. */
    private Long technicianId;

    /** Enlace a la orden viva, para abrir su detalle completo desde el historial. */
    @Column(name = "work_order_id")
    private Long workOrderId;

    // ─── Factorías de dominio ─────────────────────────────────────────────────

    /**
     * Emite el acta de una orden que acaba de cerrarse.
     *
     * <p>El desglose de la fecha y la hora de cierre en dos columnas es conocimiento
     * de ESTE contexto: antes vivía dentro de {@code WorkOrderService}, que tenía que
     * acordarse del patrón {@code hh:mm a}.
     */
    public static ServiceRecord forClosedOrder(Long workOrderId, String orderId,
                                               String client, String site, String serviceType,
                                               String technician, String technicianInitials,
                                               Long technicianId,
                                               ServiceRecordStatus status, LocalDateTime closedAt) {
        LocalDateTime at = closedAt != null ? closedAt : LocalDateTime.now();
        return ServiceRecord.builder()
                .workOrderId(workOrderId)
                .orderId(orderId)
                .completionDate(at.toLocalDate())
                .completionTime(at.format(TIME_FMT))
                .client(client)
                .site(site)
                .serviceType(serviceType)
                .technician(technician)
                .technicianInitials(technicianInitials)
                .technicianId(technicianId)
                .status(status != null ? status : ServiceRecordStatus.COMPLETED)
                .build();
    }

    /**
     * Acta cargada tal cual, con su fecha y hora ya desglosadas: datos de demostración
     * y actas anteriores al sistema, que no nacen de un evento de cierre.
     */
    public static ServiceRecord historical(String orderId, LocalDate completionDate,
                                           String completionTime, String client, String site,
                                           String serviceType, String technician,
                                           String technicianInitials, ServiceRecordStatus status,
                                           Long technicianId) {
        return ServiceRecord.builder()
                .orderId(orderId)
                .completionDate(completionDate)
                .completionTime(completionTime)
                .client(client)
                .site(site)
                .serviceType(serviceType)
                .technician(technician)
                .technicianInitials(technicianInitials)
                .technicianId(technicianId)
                .status(status != null ? status : ServiceRecordStatus.COMPLETED)
                .build();
    }

    // ─── Consultas de negocio ─────────────────────────────────────────────────

    public boolean wasCompleted() {
        return status == ServiceRecordStatus.COMPLETED;
    }

    public boolean wasSignedBy(Long candidateTechnicianId) {
        return candidateTechnicianId != null && candidateTechnicianId.equals(technicianId);
    }
}
