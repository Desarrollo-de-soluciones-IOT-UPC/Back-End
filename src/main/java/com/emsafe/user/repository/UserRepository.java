package com.emsafe.user.repository;

import com.emsafe.user.entity.AppUser;
import com.emsafe.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUser> findByRole(Role role);

    List<AppUser> findByRoleIn(List<Role> roles);
}
