package com.emsafe.device.repository;

import com.emsafe.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Device findBySerialNumber(String serialNumber);

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.client ORDER BY d.id ASC")
    List<Device> findAllWithClient();

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.client WHERE d.id = :id")
    java.util.Optional<Device> findByIdWithClient(@org.springframework.data.repository.query.Param("id") Long id);
}
