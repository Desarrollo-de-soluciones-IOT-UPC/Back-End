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

    /**
     * ¿Hay ya una alarma abierta de este tipo para este sensor?
     *
     * <p>Es lo que evita la avalancha: el edge reporta cada pocos segundos, así que un
     * sensor que se queda en DANGER una hora generaría cientos de alarmas idénticas y
     * dejaría el panel inservible. Mientras la alarma siga sin resolver, el episodio se
     * considera abierto y no se levanta otra.
     */
    boolean existsUnresolvedByTypeAndSensor(AlertType type, String sensor);

    boolean existsById(Long id);

    Alert save(Alert alert);

    List<Alert> saveAll(List<Alert> alerts);

    void deleteById(Long id);

    long count();
}
