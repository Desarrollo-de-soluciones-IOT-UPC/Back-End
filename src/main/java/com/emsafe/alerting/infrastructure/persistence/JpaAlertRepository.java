package com.emsafe.alerting.infrastructure.persistence;

import com.emsafe.alerting.domain.model.Alert;
import com.emsafe.alerting.domain.model.AlertType;
import com.emsafe.alerting.domain.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adaptador del puerto {@link AlertRepository} sobre Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class JpaAlertRepository implements AlertRepository {

    private final SpringDataAlertRepository delegate;

    @Override
    public Optional<Alert> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public List<Alert> findAllNewestFirst() {
        return delegate.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public long countByType(AlertType type) {
        return delegate.countByType(type);
    }

    @Override
    public boolean existsUnresolvedByTypeAndSensor(AlertType type, String sensor) {
        return sensor != null && delegate.existsByTypeAndSensorAndResolvedFalse(type, sensor);
    }

    @Override
    public boolean existsById(Long id) {
        return delegate.existsById(id);
    }

    @Override
    public Alert save(Alert alert) {
        return delegate.save(alert);
    }

    @Override
    public List<Alert> saveAll(List<Alert> alerts) {
        return delegate.saveAll(alerts);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
    }

    @Override
    public long count() {
        return delegate.count();
    }
}
