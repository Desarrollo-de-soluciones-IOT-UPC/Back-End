package com.emsafe.iam.infrastructure.persistence;

import com.emsafe.iam.domain.model.Role;
import com.emsafe.iam.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz Spring Data — detalle de infraestructura, no la usa el dominio.
 * El acceso pasa siempre por el puerto {@code UserRepository} vía {@link JpaUserRepository}.
 */
interface SpringDataUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);
}
