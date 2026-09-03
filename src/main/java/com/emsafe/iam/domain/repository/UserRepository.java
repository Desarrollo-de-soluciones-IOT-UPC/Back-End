package com.emsafe.iam.domain.repository;

import com.emsafe.iam.domain.model.Role;
import com.emsafe.iam.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado {@link User}.
 *
 * <p>Deliberadamente <b>sin imports de Spring</b>: el dominio define QUÉ necesita, no CÓMO
 * se implementa. El adaptador Spring Data vive en
 * {@code iam/infrastructure/persistence/JpaUserRepository}.
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsById(Long id);

    List<User> findAll();

    List<User> findAllById(List<Long> ids);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);

    long count();

    User save(User user);

    List<User> saveAll(List<User> users);

    void delete(User user);

    void deleteById(Long id);
}
