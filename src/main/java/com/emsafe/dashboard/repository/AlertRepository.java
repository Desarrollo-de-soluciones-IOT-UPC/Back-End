package com.emsafe.dashboard.repository;

import com.emsafe.dashboard.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    long countByType(String type);
}
