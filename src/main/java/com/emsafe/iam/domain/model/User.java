package com.emsafe.iam.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;
import com.emsafe.shared.domain.model.Email;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate Root del bounded context IAM: una cuenta de EMSafe (admin, técnico o cliente).
 *
 * <p>Antes se llamaba {@code AppUser} y era una bolsa de datos con {@code @Setter} en los
 * 25 campos: cualquiera podía dejarla en un estado inválido. Ahora las transiciones pasan
 * por métodos que defienden las invariantes:
 * {@link #activate()}, {@link #deactivate()}, {@link #changePassword(String)},
 * {@link #recordLogin(LocalDateTime)}, {@link #assertCanSignIn()}.
 *
 * <p><b>Persistencia sin cambios (reglas R2/R3):</b> misma tabla {@code users} y mismos
 * nombres de columna que {@code AppUser}. El único cambio es que {@code status} se mapea
 * con un {@code AttributeConverter} de aplicación automática, que guarda exactamente los
 * mismos literales en minúscula. El dominio no lo referencia: vive en infraestructura.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

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
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 100)
    private String specialty;

    @Column(length = 100)
    private String department;

    private LocalDate joinDate;

    private LocalDateTime lastLogin;

    /** Dirección registrada (legible por humanos). */
    @Column(length = 300)
    private String address;

    /** Coordenadas del sitio registrado del cliente (alimentan el mapa de radiación). */
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    /** Notas libres capturadas desde los formularios de edición del admin. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Perfil de cliente (empresa / individuo) ───────────────────────────────
    @Column(name = "client_type", length = 20)
    private String clientType;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(length = 50)
    private String industry;

    @Column(length = 100)
    private String country;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    // ─── Factorías de dominio ─────────────────────────────────────────────────

    /**
     * Registro público desde la app móvil: el cliente queda PENDING y no puede
     * iniciar sesión hasta que un administrador lo active.
     */
    public static User registerClient(String name, String rawEmail, String passwordHash,
                                      String phone, String address, String clientType,
                                      String contactName, String taxId, String industry) {
        String normalizedEmail = Email.of(rawEmail).value();
        return User.builder()
                .name(name.trim())
                .initials(Initials.fromFirstWords(name))
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .role(Role.CLIENT)
                .phone(phone)
                .address(address)
                .clientType(clientType)
                .contactName(contactName)
                .contactEmail(normalizedEmail)
                .contactPhone(phone)
                .taxId(taxId)
                .industry(industry)
                .joinDate(LocalDate.now())
                .status(UserStatus.PENDING)
                .build();
    }

    /** Alta desde el portal admin: la cuenta nace activa. */
    public static User createByAdmin(String name, String initials, String rawEmail,
                                     String passwordHash, Role role, String phone,
                                     String location, String specialty, String department,
                                     LocalDate joinDate, String notes) {
        return User.builder()
                .name(name)
                .initials(initials != null && !initials.isBlank()
                        ? initials
                        : Initials.fromFirstAndLastWord(name))
                .email(Email.of(rawEmail).value())
                .passwordHash(passwordHash)
                .role(role)
                .phone(phone)
                .location(location)
                .specialty(specialty)
                .department(department)
                .joinDate(joinDate != null ? joinDate : LocalDate.now())
                .status(UserStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    // ─── Comportamiento de negocio ────────────────────────────────────────────

    public boolean isActive() {
        return status.canSignIn();
    }

    public boolean isPendingApproval() {
        return status == UserStatus.PENDING;
    }

    /**
     * Invariante de acceso: solo una cuenta ACTIVE puede autenticarse. Distingue el
     * motivo para que el usuario reciba un mensaje accionable en vez de un genérico.
     */
    public void assertCanSignIn() {
        if (status.canSignIn()) {
            return;
        }
        if (status == UserStatus.PENDING) {
            throw new BadRequestException("Your account is pending approval by an administrator.");
        }
        throw new BadRequestException("Your account has been deactivated. Please contact an administrator.");
    }

    /** Aprobación de un registro pendiente, o reactivación de una cuenta desactivada. */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void changeStatus(UserStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("Status is required");
        }
        this.status = newStatus;
    }

    /** Sella el hash ya cifrado. El cifrado es responsabilidad de la capa de aplicación. */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new BadRequestException("Password hash is required");
        }
        this.passwordHash = newPasswordHash;
    }

    public void recordLogin(LocalDateTime when) {
        this.lastLogin = when;
    }

    public void rename(String newName, String newInitials) {
        if (newName != null && !newName.isBlank()) {
            this.name = newName;
        }
        if (newInitials != null && !newInitials.isBlank()) {
            this.initials = newInitials;
        }
    }

    /** Cambia el email validando el formato; la unicidad la comprueba el repositorio. */
    public void changeEmail(String rawEmail) {
        this.email = Email.of(rawEmail).value();
    }

    public boolean hasEmail(String rawEmail) {
        return rawEmail != null && this.email != null
                && this.email.equalsIgnoreCase(rawEmail.trim());
    }

    public void updateStaffDetails(String phone, String location, String specialty,
                                   String department, LocalDate joinDate, String notes) {
        if (phone != null && !phone.isBlank()) this.phone = phone;
        if (location != null && !location.isBlank()) this.location = location;
        if (specialty != null && !specialty.isBlank()) this.specialty = specialty;
        if (department != null && !department.isBlank()) this.department = department;
        if (joinDate != null) this.joinDate = joinDate;
        if (notes != null) this.notes = notes;
    }

    /** Perfil de cliente: se permite limpiar con cadena vacía (no con null). */
    public void updateClientProfile(String address, String clientType, String taxId,
                                    String industry, String country, String contactName,
                                    String contactEmail, String contactPhone) {
        if (address != null) this.address = address;
        if (clientType != null) this.clientType = clientType;
        if (taxId != null) this.taxId = taxId;
        if (industry != null) this.industry = industry;
        if (country != null) this.country = country;
        if (contactName != null) this.contactName = contactName;
        if (contactEmail != null) this.contactEmail = contactEmail;
        if (contactPhone != null) this.contactPhone = contactPhone;
    }

    /** Coordenadas del selector de mapa; alimentan el mapa de radiación por cliente. */
    public void relocate(Double latitude, Double longitude) {
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }
}
