package com.emsafe.device.domain.model;

import com.emsafe.iam.domain.model.User;
import com.emsafe.shared.domain.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate Root del bounded context Device: un sensor EMSafe instalado en campo.
 *
 * <p>Antes era una bolsa de datos con {@code @Setter}: la lógica de instalación,
 * recolección y mantenimiento vivía dispersa en {@code WorkOrderService} (que
 * manipulaba directamente los devices de otro contexto) y en {@code DeviceService}.
 * Ahora esas transiciones son métodos del agregado, con sus invariantes:
 * {@link #claimFor}, {@link #releaseToPool()}, {@link #startMaintenance()},
 * {@link #orderPlug(PlugState)}.
 *
 * <p><b>Persistencia sin cambios (R2/R3):</b> misma tabla {@code devices} y mismas
 * columnas. {@code status} y {@code desired_plug} se mapean con converters de
 * aplicación automática que guardan los literales de siempre.
 *
 * <p><b>Decisión de alcance:</b> se conserva la asociación JPA {@code @ManyToOne User}
 * hacia el contexto IAM en vez de una referencia por ID. Es un compromiso consciente
 * del DDD pragmático en un monolito modular: cambiarla obligaría a rehacer los
 * {@code JOIN FETCH} y los DTOs que muestran el nombre del cliente, con riesgo de N+1
 * y sin ganancia real mientras ambos contextos compartan base de datos.
 */
@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(length = 100)
    private String serialNumber;

    @Column
    private LocalDate installDate;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Estado DESEADO del relé, ordenado por el usuario desde la app.
     * El edge lo consulta (GET /api/v1/devices/{serial}/plug) para accionar el
     * dispositivo; el estado real reportado viaja en cada lectura (reading.plug).
     */
    @Column(name = "desired_plug", length = 8)
    private PlugState desiredPlug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    // ─── Factorías de dominio ─────────────────────────────────────────────────

    /**
     * Sensor descubierto por el edge que aún no está registrado: se crea en el pool
     * (sin cliente) para no perder telemetría. Un técnico lo reclamará más tarde.
     */
    public static Device discoveredByEdge(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new BadRequestException("serialNumber is required");
        }
        return Device.builder()
                .name("Unregistered sensor " + serialNumber.trim())
                .type("Sensor")
                .status(DeviceStatus.UNREGISTERED)
                .serialNumber(serialNumber.trim())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** Alta manual desde el portal de administración. */
    public static Device register(String name, String type, String location,
                                  DeviceStatus status, String serialNumber,
                                  LocalDate installDate, User client) {
        return Device.builder()
                .name(name)
                .type(type)
                .location(location)
                .status(status != null ? status : DeviceStatus.ACTIVE)
                .serialNumber(serialNumber)
                .installDate(installDate)
                .client(client)
                .build();
    }

    // ─── Comportamiento de negocio ────────────────────────────────────────────

    public boolean isUnregistered() {
        return status.isUnregistered();
    }

    public boolean belongsTo(Long clientId) {
        return clientId != null && client != null && clientId.equals(client.getId());
    }

    /**
     * Instalación: el técnico reclama un sensor del pool y lo deja operativo en la
     * sede del cliente. Nombre y tipo son opcionales; si no vienen se conserva lo
     * que ya tuviera (o un valor por defecto si el edge lo autocreó sin datos).
     */
    public void claimFor(User newClient, String siteLocation, String newName, String newType) {
        if (newName != null && !newName.isBlank()) {
            this.name = newName;
        } else if (this.name == null || this.name.isBlank()) {
            this.name = "Sensor";
        }
        if (newType != null && !newType.isBlank()) {
            this.type = newType;
        } else if (this.type == null || this.type.isBlank()) {
            this.type = "Sensor";
        }
        this.client = newClient;
        this.location = siteLocation;
        this.status = DeviceStatus.ACTIVE;
        this.installDate = LocalDate.now();
    }

    /**
     * Recolección: el sensor vuelve al pool. Se limpian cliente y fecha de
     * instalación para que pueda volver a descubrirse en otra sede.
     */
    public void releaseToPool() {
        this.status = DeviceStatus.UNREGISTERED;
        this.client = null;
        this.installDate = null;
    }

    /** Se detectó una avería: queda a la espera de una orden de mantenimiento. */
    public void flagForMaintenance() {
        this.status = DeviceStatus.REQUIRES_MAINTENANCE;
    }

    /** El técnico arranca la orden de mantenimiento sobre este sensor. */
    public void startMaintenance() {
        this.status = DeviceStatus.IN_MAINTENANCE;
    }

    /**
     * Cambio de estado genérico (portal admin y parte de técnico). Recolección y
     * mantenimiento tienen sus propios métodos porque arrastran efectos extra.
     */
    public void changeStatus(DeviceStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("Device status is required");
        }
        if (newStatus == DeviceStatus.UNREGISTERED) {
            releaseToPool();
            return;
        }
        this.status = newStatus;
    }

    /**
     * Orden del usuario para el relé (camino mobile → backend → edge).
     * Se acepta null para "sin orden".
     */
    public void orderPlug(PlugState desired) {
        this.desiredPlug = desired;
    }

    public void assignTo(User newClient) {
        this.client = newClient;
    }

    public void updateDetails(String newName, String newType, String newLocation,
                              String newSerialNumber, LocalDate newInstallDate) {
        if (newName != null && !newName.isBlank()) this.name = newName;
        if (newType != null && !newType.isBlank()) this.type = newType;
        if (newLocation != null) this.location = newLocation;
        if (newSerialNumber != null) this.serialNumber = newSerialNumber;
        if (newInstallDate != null) this.installDate = newInstallDate;
    }
}
