package com.emsafe.alerting.domain.repository;

import com.emsafe.alerting.domain.model.Alert;
import com.emsafe.alerting.domain.model.AlertType;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia del agregado {@link Alert}. */
public interface AlertRepository {

    Optional<Alert> findById(Long id);

    List<Alert> findAllNewestFirst();

    long countByType(AlertType type);

    boolean existsById(Long id);

    Alert save(Alert alert);

    List<Alert> saveAll(List<Alert> alerts);

    void deleteById(Long id);

    long count();
}
