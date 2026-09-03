package com.emsafe.monitoring.infrastructure.persistence;

import com.emsafe.monitoring.domain.model.RadiationReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Interfaz Spring Data — infraestructura; el dominio solo ve el puerto. */
interface SpringDataRadiationReadingRepository extends JpaRepository<RadiationReading, Long> {

    @Query("SELECT r FROM RadiationReading r ORDER BY r.readingDate ASC")
    List<RadiationReading> findAllSorted();

    @Query("SELECT r FROM RadiationReading r LEFT JOIN FETCH r.device d LEFT JOIN FETCH d.client ORDER BY r.readingDate ASC")
    List<RadiationReading> findAllWithDeviceAndClient();

    @Query("SELECT AVG(r.value) FROM RadiationReading r")
    Double findAverage();

    @Query("SELECT r FROM RadiationReading r JOIN FETCH r.device d WHERE d.client.id = :clientId ORDER BY r.recordedAt DESC, r.id DESC")
    List<RadiationReading> findByClient(@Param("clientId") Long clientId);

    @Query("SELECT r FROM RadiationReading r JOIN FETCH r.device d WHERE d.id = :deviceId ORDER BY r.recordedAt DESC, r.id DESC")
    List<RadiationReading> findByDevice(@Param("deviceId") Long deviceId);

    @Query("SELECT r FROM RadiationReading r LEFT JOIN FETCH r.device d LEFT JOIN FETCH d.client"
            + " ORDER BY r.recordedAt DESC, r.id DESC")
    List<RadiationReading> findRecent(Pageable pageable);

    @Query("SELECT r FROM RadiationReading r LEFT JOIN FETCH r.device d LEFT JOIN FETCH d.client"
            + " WHERE r.sensorId = :serialNumber ORDER BY r.recordedAt DESC, r.id DESC")
    List<RadiationReading> findBySensorId(@Param("serialNumber") String serialNumber, Pageable pageable);

    long countBySensorId(String sensorId);
}
