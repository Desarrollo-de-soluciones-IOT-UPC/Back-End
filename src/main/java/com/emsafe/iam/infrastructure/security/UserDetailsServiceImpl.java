package com.emsafe.iam.infrastructure.security;

import com.emsafe.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adaptador entre el agregado {@code User} del dominio IAM y el modelo de Spring Security.
 *
 * <p>Ojo con los dos "User" en juego: el del dominio ({@code com.emsafe.iam.domain.model.User})
 * y el de Spring ({@code org.springframework.security.core.userdetails.User}). El de Spring se
 * referencia con su nombre completamente cualificado para que no haya ambigüedad.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .<UserDetails>map(u -> org.springframework.security.core.userdetails.User.builder()
                        .username(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        // Solo las cuentas ACTIVE operan: "pending"/"inactive" quedan
                        // deshabilitadas, así un JWT todavía vigente deja de servir en
                        // cuanto un admin desactiva la cuenta.
                        .disabled(!u.isActive())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
