package com.emsafe.workorder.repository;

import com.emsafe.workorder.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    List<Sensor> findByWorkOrderId(Long workOrderId);

    long countByWorkOrderId(Long workOrderId);
}
