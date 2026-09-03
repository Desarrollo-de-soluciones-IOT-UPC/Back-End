package com.emsafe.workorder.domain.model;

import com.emsafe.iam.domain.model.User;
import com.emsafe.shared.domain.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root del bounded context WorkOrder: una orden de trabajo de campo.
 *
 * <p>Este agregado es el que más ganó con la migración. Antes era una bolsa de datos
 * con {@code @Setter} en 25 campos, y las reglas vivían en un servicio de 512 líneas
 * que además inyectaba <b>cinco repositorios de cuatro contextos distintos</b>. Ahora
 * las transiciones del ciclo de vida son métodos con invariantes:
 * {@link #start()}, {@link #complete(LocalDateTime)}, {@link #cancel(String)} y
 * {@link #assignTechnician(User)}; la máquina de estados la valida
 * {@link WorkOrderStatus#canTransitionTo}.
 *
 * <p>Contiene entidades internas — {@code Sensor}, {@code ActivityLogEntry},
 * {@code WorkOrderEvidence}, {@code MaintenanceAction} — alcanzables solo a través de
 * esta raíz: ninguna tiene repositorio propio.
 */
@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.PENDING;

    @Column(nullable = false, length = 150)
    private String client;

    // Client reference (nullable — legacy orders only carry the client name string).
    // Used to resolve the client's devices in Maintenance / Collection flows.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id")
    private User clientUser;

    @Column(length = 200)
    private String location;

    @Column(length = 100)
    private String city;

    private LocalDate scheduledDate;

    @Column(length = 20)
    private String scheduledTime;

    // Technician reference (nullable — may not be assigned yet)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    // Denormalized for quick display without JOIN
    @Column(length = 100)
    private String technicianName;

    @Column(length = 5)
    private String technicianInitials;

    @Column(length = 30)
    private String priority;

    @Column(length = 100)
    private String contactName;

    @Column(length = 100)
    private String contactRole;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 150)
    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String accessInstructions;

    private Integer expectedSensors;

    @Column(length = 50)
    private String assetId;

    @Column(columnDefinition = "TEXT")
    private String technicianNotes;

    // Timestamp set when the work order is marked Completed by the technician.
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Reason captured when an order is Cancelled / Deleted by the admin.
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @ElementCollection
    @CollectionTable(name = "work_order_tools",
            joinColumns = @JoinColumn(name = "work_order_id"))
    @Column(name = "tool")
    @BatchSize(size = 30)
    @Builder.Default
    private List<String> requiredTools = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @BatchSize(size = 30)
    @Builder.Default
    private List<Sensor> sensors = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<ActivityLogEntry> activityLog = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<WorkOrderEvidence> evidence = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<MaintenanceAction> maintenanceActions = new ArrayList<>();

    // ─── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Sella el código legible ({@code #WO-0042}) una vez que la BD asignó el id.
     * Solo tiene efecto la primera vez.
     */
    public void assignOrderId() {
        if (this.orderId == null && this.id != null) {
            this.orderId = OrderId.forSequence(this.id);
        }
    }

    /**
     * Cambia el estado validando la máquina de estados. Es la ÚNICA puerta de
     * cambio: {@link #start()}, {@link #complete} y {@link #cancel} pasan por aquí.
     *
     * @throws BadRequestException si la transición no está permitida
     */
    public void changeStatus(WorkOrderStatus target) {
        if (target == null) {
            throw new BadRequestException("Status is required");
        }
        if (!this.status.canTransitionTo(target)) {
            throw new BadRequestException("Invalid status transition: " + this.status + " → " + target);
        }
        this.status = target;
    }

    /** El técnico arranca el trabajo: PENDING → IN_PROGRESS. */
    public void start() {
        changeStatus(WorkOrderStatus.IN_PROGRESS);
    }

    /**
     * Trabajo terminado: IN_PROGRESS → COMPLETED y se sella la fecha de cierre.
     * El sellado solo ocurre la primera vez, para no falsear el histórico.
     */
    public void complete(LocalDateTime when) {
        changeStatus(WorkOrderStatus.COMPLETED);
        if (this.completedAt == null) {
            this.completedAt = when != null ? when : LocalDateTime.now();
        }
    }

    /**
     * Cancelación (lo que el admin ve como "eliminar"): la orden se conserva para
     * que su detalle siga disponible desde el historial, pero sale de las listas
     * activas. El motivo es opcional; si viene, se guarda.
     */
    public void cancel(String reason) {
        changeStatus(WorkOrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            this.cancellationReason = reason;
        }
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean isCompleted() {
        return status == WorkOrderStatus.COMPLETED;
    }

    /**
     * ¿Este cambio de estado arranca un mantenimiento? Es la señal para que los
     * sensores averiados del cliente pasen a "in-maintenance".
     */
    public boolean startsMaintenance(WorkOrderStatus previous) {
        return previous == WorkOrderStatus.PENDING
                && status == WorkOrderStatus.IN_PROGRESS
                && type == WorkOrderType.MAINTENANCE
                && clientUser != null;
    }

    // ─── Asignación ───────────────────────────────────────────────────────────

    /**
     * Asigna técnico y mantiene coherentes los campos denormalizados que el listado
     * usa para pintar sin JOIN. Antes eran tres setters sueltos que había que recordar.
     */
    public void assignTechnician(User newTechnician) {
        this.technician = newTechnician;
        this.technicianName = newTechnician != null ? newTechnician.getName() : null;
        this.technicianInitials = newTechnician != null ? newTechnician.getInitials() : null;
    }

    public void unassignTechnician() {
        assignTechnician(null);
    }

    public void linkClient(User client) {
        this.clientUser = client;
    }

    public Long technicianId() {
        return technician != null ? technician.getId() : null;
    }

    public Long clientUserId() {
        return clientUser != null ? clientUser.getId() : null;
    }

    /** ¿Está asignada a este técnico? Base del control de acceso del portal técnico. */
    public boolean isAssignedTo(Long candidateTechnicianId) {
        return candidateTechnicianId != null
                && technician != null
                && candidateTechnicianId.equals(technician.getId());
    }

    // ─── Contenido de la orden ────────────────────────────────────────────────

    public void appendNotes(String notes) {
        if (notes != null && !notes.isBlank()) {
            this.technicianNotes = notes;
        }
    }

    public void reschedule(LocalDate date, String time) {
        if (date != null) this.scheduledDate = date;
        if (time != null) this.scheduledTime = time;
    }

    /** Añade o actualiza un sensor de la orden, identificándolo por su serial. */
    public void recordSensor(String targetSensorId, String sensorLocation, String sensorStatus) {
        sensors.stream()
                .filter(existing -> existing.getSensorId() != null
                        && existing.getSensorId().equals(targetSensorId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setStatus(sensorStatus),
                        () -> sensors.add(Sensor.builder()
                                .sensorId(targetSensorId)
                                .location(sensorLocation)
                                .status(sensorStatus)
                                .workOrder(this)
                                .build()));
    }

    public void recordMaintenanceAction(Long deviceId, String deviceName,
                                        String action, String description) {
        if (action == null || action.isBlank()) {
            return;
        }
        maintenanceActions.add(MaintenanceAction.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .action(action)
                .description(description)
                .workOrder(this)
                .build());
    }

    public void attachEvidence(String image) {
        if (image == null || image.isBlank()) {
            return;
        }
        evidence.add(WorkOrderEvidence.builder()
                .image(image)
                .workOrder(this)
                .build());
    }

    public void logActivity(String event, String time) {
        activityLog.add(ActivityLogEntry.builder()
                .event(event)
                .logTime(time)
                .workOrder(this)
                .build());
    }
}
