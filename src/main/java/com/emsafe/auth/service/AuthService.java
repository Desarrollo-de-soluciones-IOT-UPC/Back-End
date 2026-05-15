package com.emsafe.auth.service;

import com.emsafe.auth.dto.LoginRequest;
import com.emsafe.auth.dto.LoginResponse;
import com.emsafe.auth.dto.RefreshResponse;
import com.emsafe.auth.security.JwtUtil;
import com.emsafe.auth.security.LoginAttemptService;
import com.emsafe.user.entity.AppUser;
import com.emsafe.user.entity.Role;
import com.emsafe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public RefreshResponse refresh(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "").trim();
        if (!jwtUtil.isTokenValid(token)) {
            throw new BadCredentialsException("Token is invalid or expired");
        }
        String email = jwtUtil.extractEmail(token);
        String role  = jwtUtil.extractRole(token);
        Long userId  = jwtUtil.extractUserId(token);
        String newToken = jwtUtil.generateToken(email, role, userId);
        return new RefreshResponse(newToken, 86400000L);
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        if (loginAttemptService.isBlocked(req.email())) {
            long secs = loginAttemptService.secondsUntilUnlock(req.email());
            throw new BadCredentialsException("Account temporarily locked. Try again in " + secs + " seconds.");
        }

        try {
            AppUser user = userRepository.findByEmail(req.email())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid email or password");
            }

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

            Long technicianId = user.getRole() == Role.TECHNICIAN ? user.getId() : null;

            loginAttemptService.registerSuccess(req.email());

            return new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getName(),
                    user.getInitials(),
                    user.getRole().name().toLowerCase(),
                    user.getId(),
                    technicianId
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.registerFailure(req.email());
            throw ex;
        }
    }
}
