package com.emsafe.iam.application;

import com.emsafe.iam.domain.event.UserActivated;
import com.emsafe.iam.domain.event.UserDeactivated;
import com.emsafe.iam.domain.event.UserDeleted;
import com.emsafe.iam.domain.model.Role;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.model.UserStatus;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.iam.interfaces.rest.dto.ChangePasswordRequest;
import com.emsafe.iam.interfaces.rest.dto.CreateUserRequest;
import com.emsafe.iam.interfaces.rest.dto.UpdateUserRequest;
import com.emsafe.iam.interfaces.rest.dto.UserDto;
import com.emsafe.shared.domain.event.DomainEventPublisher;
import com.emsafe.shared.domain.exception.BadRequestException;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Casos de uso de gestión de cuentas del contexto IAM.
 *
 * <p>Toda la mutación del agregado pasa por métodos de negocio de {@link User}
 * ({@code rename}, {@code changeEmail}, {@code activate}...): ya no hay setters sueltos.
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher events;

    public List<UserDto> findAll(String role) {
        List<User> users = StringUtils.hasText(role)
                ? userRepository.findByRole(Role.fromApi(role))
                : userRepository.findAll();
        return users.stream().map(UserDto::from).toList();
    }

    public UserDto findById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use: " + req.email());
        }

        User user = User.createByAdmin(
                req.name(),
                req.initials(),
                req.email(),
                passwordEncoder.encode(req.password()),
                req.role(),
                req.phone(),
                req.location(),
                req.specialty(),
                req.department(),
                req.joinDate(),
                req.notes()
        );

        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.rename(req.name(), req.initials());

        if (StringUtils.hasText(req.email()) && !user.hasEmail(req.email())) {
            if (userRepository.existsByEmail(req.email())) {
                throw new BadRequestException("Email already in use: " + req.email());
            }
            user.changeEmail(req.email());
        }

        user.updateStaffDetails(req.phone(), req.location(), req.specialty(),
                req.department(), req.joinDate(), req.notes());

        user.updateClientProfile(req.address(), req.clientType(), req.taxId(),
                req.industry(), req.country(), req.contactName(),
                req.contactEmail(), req.contactPhone());

        // Coordenadas del selector de mapa (el mapa de radiación depende de ellas).
        user.relocate(req.latitude(), req.longitude());

        if (StringUtils.hasText(req.password())) {
            user.changePassword(passwordEncoder.encode(req.password()));
        }

        applyStatusChange(user, req.status());

        return UserDto.from(userRepository.save(user));
    }

    /**
     * Aplica el cambio de estado y emite el evento correspondiente, para que otros
     * contextos reaccionen sin que IAM los conozca.
     */
    private void applyStatusChange(User user, String requestedStatus) {
        if (!StringUtils.hasText(requestedStatus)) {
            return;
        }
        UserStatus target = UserStatus.fromPersisted(requestedStatus);
        if (target == user.getStatus()) {
            return;
        }
        user.changeStatus(target);
        if (target == UserStatus.ACTIVE) {
            events.publish(new UserActivated(user.getId(), user.getEmail()));
        } else if (target == UserStatus.INACTIVE) {
            events.publish(new UserDeactivated(user.getId(), user.getEmail()));
        }
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        String email = user.getEmail();
        userRepository.deleteById(id);
        events.publish(new UserDeleted(id, email));
    }

    // ─── Autoservicio del propio usuario (app móvil) ──────────────────────────
    // Casos de uso en los que el actor y el sujeto son la MISMA persona. Viven en
    // IAM, no en el portal móvil, porque gestionar una cuenta es de este contexto.

    /**
     * El cliente edita su propio perfil desde la app. Email, rol y estado quedan fuera
     * a propósito: cambiarlos no es autoservicio.
     */
    @Transactional
    public void updateOwnClientProfile(Long id, String name, String phone, String location,
                                       String address, Double latitude, Double longitude) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.rename(name, null);
        user.updateStaffDetails(phone, location, null, null, null, null);
        if (StringUtils.hasText(address)) {
            user.updateClientProfile(address, null, null, null, null, null, null, null);
        }
        user.relocate(latitude, longitude);
        userRepository.save(user);
    }

    /**
     * Baja voluntaria de la propia cuenta (derecho al olvido), confirmada con la
     * contraseña actual. Los sensores del cliente se desvinculan solos
     * (FK {@code ON DELETE SET NULL}) y las lecturas quedan como dato anónimo.
     *
     * <p>Antes esto lo hacía el portal móvil llamando a {@code userRepository.delete()}
     * por su cuenta, y por eso <b>no emitía {@code UserDeleted}</b>: la baja desde la app
     * era invisible para el resto del sistema, mientras que la del admin sí se anunciaba.
     * Ahora los dos caminos pasan por aquí y publican el mismo evento.
     */
    @Transactional
    public void deleteOwnAccount(Long id, String password) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Password is incorrect");
        }
        String email = user.getEmail();
        userRepository.deleteById(id);
        events.publish(new UserDeleted(id, email));
    }
}
