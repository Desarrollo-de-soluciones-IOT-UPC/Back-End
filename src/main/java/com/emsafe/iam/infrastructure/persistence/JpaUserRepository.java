package com.emsafe.iam.infrastructure.persistence;

import com.emsafe.iam.domain.model.Role;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa el puerto {@link UserRepository} sobre Spring Data JPA.
 *
 * <p>Es la única clase que conoce ambos mundos: gracias a ella, el dominio y la capa de
 * aplicación no importan nada de Spring Data.
 */
@Component
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository delegate;

    @Override
    public Optional<User> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return delegate.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return delegate.existsByEmail(email);
    }

    @Override
    public boolean existsById(Long id) {
        return delegate.existsById(id);
    }

    @Override
    public List<User> findAll() {
        return delegate.findAll();
    }

    @Override
    public List<User> findAllById(List<Long> ids) {
        return delegate.findAllById(ids);
    }

    @Override
    public List<User> findByRole(Role role) {
        return delegate.findByRole(role);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public List<User> findByRoleIn(List<Role> roles) {
        return delegate.findByRoleIn(roles);
    }

    @Override
    public User save(User user) {
        return delegate.save(user);
    }

    @Override
    public List<User> saveAll(List<User> users) {
        return delegate.saveAll(users);
    }

    @Override
    public void delete(User user) {
        delegate.delete(user);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
    }
}
