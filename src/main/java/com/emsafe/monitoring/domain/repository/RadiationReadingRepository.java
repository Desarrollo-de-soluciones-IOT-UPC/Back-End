package com.emsafe.monitoring.domain.repository;

import com.emsafe.monitoring.domain.model.RadiationReading;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado {@link RadiationReading}.
 *
 * <p>La paginación de Spring se sustituye por un simple {@code limit}: el dominio
 * expresa "dame las N más recientes" sin conocer {@code Pageable}.
 */
public interface RadiationReadingRepository {

    List<RadiationReading> findAllSorted();

    List<RadiationReading> findAllWithDeviceAndClient();

    Double findAverageValue();

    List<RadiationReading> findByClient(Long clientId);

    List<RadiationReading> findByDevice(Long deviceId);

    /** Las {@code limit} lecturas más recientes de cualquier sensor. */
    List<RadiationReading> findRecent(int limit);

    /** Las {@code limit} lecturas más recientes de un sensor concreto. */
    List<RadiationReading> findBySensorId(String serialNumber, int limit);

    /** Última lectura de un sensor, si existe. */
    Optional<RadiationReading> findLatestBySensorId(String serialNumber);

    long countBySensorId(String serialNumber);

    RadiationReading save(RadiationReading reading);

    List<RadiationReading> saveAll(List<RadiationReading> readings);

    List<RadiationReading> findAll();
}
