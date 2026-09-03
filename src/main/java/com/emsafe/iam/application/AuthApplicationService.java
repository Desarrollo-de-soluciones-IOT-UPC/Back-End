package com.emsafe.iam.application;

import com.emsafe.iam.domain.event.UserRegistered;
import com.emsafe.iam.domain.model.Role;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.iam.infrastructure.security.JwtUtil;
import com.emsafe.iam.infrastructure.security.LoginAttemptService;
import com.emsafe.iam.interfaces.rest.dto.LoginRequest;
import com.emsafe.iam.interfaces.rest.dto.LoginResponse;
import com.emsafe.iam.interfaces.rest.dto.RefreshResponse;
import com.emsafe.iam.interfaces.rest.dto.RegisterRequest;
import com.emsafe.shared.domain.event.DomainEventPublisher;
import com.emsafe.shared.domain.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Casos de uso de autenticación del contexto IAM.
 *
 * <p>Orquesta; no decide. Las reglas de negocio (¿puede esta cuenta iniciar sesión?,
 * ¿cómo se construye un cliente recién registrado?) viven en el agregado {@link User}.
 */
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final DomainEventPublisher events;

    public RefreshResponse refresh(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "").trim();
        if (!jwtUtil.isTokenValid(token)) {
            throw new BadCredentialsException("Token is invalid or expired");
        }
        // Se relee la cuenta: un usuario borrado o desactivado (o con el rol cambiado)
        // no puede renovar su token indefinidamente.
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is not active");
        }
        String newToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new RefreshResponse(newToken, jwtUtil.getExpirationMs());
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        if (loginAttemptService.isBlocked(req.email())) {
            long secs = loginAttemptService.secondsUntilUnlock(req.email());
            // LockedException (y no BadCredentialsException) para que el
            // GlobalExceptionHandler muestre el motivo real del bloqueo.
            throw new LockedException("Account temporarily locked. Try again in " + secs + " seconds.");
        }

        try {
            User user = userRepository.findByEmail(req.email())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid email or password");
            }

            // Invariante del agregado: solo las cuentas activas entran. Distingue
            // "pendiente de aprobación" de "desactivada" con su propio mensaje.
            user.assertCanSignIn();

            user.recordLogin(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
            Long technicianId = user.getRole() == Role.TECHNICIAN ? user.getId() : null;

            loginAttemptService.registerSuccess(req.email());

            return new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getName(),
                    user.getInitials(),
                    user.getRole().apiValue(),
                    user.getId(),
                    technicianId
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.registerFailure(req.email());
            throw ex;
        }
    }

    /**
     * Registro público desde la app móvil: crea un CLIENT en estado "pending".
     * La cuenta no puede iniciar sesión hasta que un admin la active en el portal web.
     */
    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use: " + req.email());
        }

        User user = userRepository.save(User.registerClient(
                req.name(),
                req.email(),
                passwordEncoder.encode(req.password()),
                req.phone(),
                req.address(),
                req.clientType(),
                req.contactName(),
                req.taxId(),
                req.industry()
        ));

        events.publish(new UserRegistered(user.getId(), user.getEmail(), user.getName()));
    }
}
