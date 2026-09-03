package com.emsafe.device.infrastructure.persistence;

import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.model.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Interfaz Spring Data — detalle de infraestructura; el dominio no la ve. */
interface SpringDataDeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findBySerialNumber(String serialNumber);

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.client ORDER BY d.id ASC")
    List<Device> findAllWithClient();

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.client WHERE d.id = :id")
    Optional<Device> findByIdWithClient(@Param("id") Long id);

    List<Device> findByClient_IdOrderByIdAsc(Long clientId);

    Optional<Device> findByIdAndClient_Id(Long id, Long clientId);

    List<Device> findByClient_IdAndStatusOrderByIdAsc(Long clientId, DeviceStatus status);

    List<Device> findByStatusOrderByIdAsc(DeviceStatus status);

    long countByStatus(DeviceStatus status);
}
