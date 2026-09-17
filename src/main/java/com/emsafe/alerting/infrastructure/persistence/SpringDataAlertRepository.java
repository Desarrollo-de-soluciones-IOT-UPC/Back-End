package com.emsafe.alerting.infrastructure.persistence;

import com.emsafe.alerting.domain.model.Alert;
import com.emsafe.alerting.domain.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Interfaz Spring Data — infraestructura. */
interface SpringDataAlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    long countByType(AlertType type);

    boolean existsByTypeAndSensorAndResolvedFalse(AlertType type, String sensor);
}
