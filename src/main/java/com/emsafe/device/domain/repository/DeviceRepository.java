package com.emsafe.device.domain.repository;

import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.model.DeviceStatus;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado {@link Device}. Sin imports de Spring.
 * Adaptador: {@code device/infrastructure/persistence/JpaDeviceRepository}.
 */
public interface DeviceRepository {

    Optional<Device> findById(Long id);

    /** Carga el device con su cliente ya resuelto (evita el N+1 al construir el DTO). */
    Optional<Device> findByIdWithClient(Long id);

    Optional<Device> findBySerialNumber(String serialNumber);

    List<Device> findAllWithClient();

    List<Device> findByClient(Long clientId);

    Optional<Device> findByIdAndClient(Long id, Long clientId);

    List<Device> findByClientAndStatus(Long clientId, DeviceStatus status);

    List<Device> findByStatus(DeviceStatus status);

    long countByStatus(DeviceStatus status);

    boolean existsById(Long id);

    Device save(Device device);

    List<Device> saveAll(List<Device> devices);

    void deleteById(Long id);
}
