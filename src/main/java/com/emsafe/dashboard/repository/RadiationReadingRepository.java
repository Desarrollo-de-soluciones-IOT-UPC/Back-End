package com.emsafe.dashboard.repository;

import com.emsafe.dashboard.entity.RadiationReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RadiationReadingRepository extends JpaRepository<RadiationReading, Long> {

    @Query("SELECT r FROM RadiationReading r ORDER BY r.readingDate ASC")
    List<RadiationReading> findAllSorted();

    @Query("SELECT r FROM RadiationReading r LEFT JOIN FETCH r.device d LEFT JOIN FETCH d.client ORDER BY r.readingDate ASC")
    List<RadiationReading> findAllWithDeviceAndClient();

    @Query("SELECT AVG(r.value) FROM RadiationReading r")
    Double findAverage();
}
