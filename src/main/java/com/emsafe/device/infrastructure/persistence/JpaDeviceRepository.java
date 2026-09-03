package com.emsafe.device.infrastructure.persistence;

import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.model.DeviceStatus;
import com.emsafe.device.domain.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adaptador del puerto {@link DeviceRepository} sobre Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class JpaDeviceRepository implements DeviceRepository {

    private final SpringDataDeviceRepository delegate;

    @Override
    public Optional<Device> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<Device> findByIdWithClient(Long id) {
        return delegate.findByIdWithClient(id);
    }

    @Override
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return delegate.findBySerialNumber(serialNumber);
    }

    @Override
    public List<Device> findAllWithClient() {
        return delegate.findAllWithClient();
    }

    @Override
    public List<Device> findByClient(Long clientId) {
        return delegate.findByClient_IdOrderByIdAsc(clientId);
    }

    @Override
    public Optional<Device> findByIdAndClient(Long id, Long clientId) {
        return delegate.findByIdAndClient_Id(id, clientId);
    }

    @Override
    public List<Device> findByClientAndStatus(Long clientId, DeviceStatus status) {
        return delegate.findByClient_IdAndStatusOrderByIdAsc(clientId, status);
    }

    @Override
    public List<Device> findByStatus(DeviceStatus status) {
        return delegate.findByStatusOrderByIdAsc(status);
    }

    @Override
    public long countByStatus(DeviceStatus status) {
        return delegate.countByStatus(status);
    }

    @Override
    public boolean existsById(Long id) {
        return delegate.existsById(id);
    }

    @Override
    public Device save(Device device) {
        return delegate.save(device);
    }

    @Override
    public List<Device> saveAll(List<Device> devices) {
        return delegate.saveAll(devices);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
    }
}
