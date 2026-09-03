package com.emsafe.monitoring.infrastructure.persistence;

import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador del puerto {@link RadiationReadingRepository}.
 *
 * <p>Aquí es donde el "dame las N más recientes" del dominio se traduce al
 * {@code Pageable} de Spring Data: ese detalle no sale de infraestructura.
 */
@Component
@RequiredArgsConstructor
public class JpaRadiationReadingRepository implements RadiationReadingRepository {

    private final SpringDataRadiationReadingRepository delegate;

    @Override
    public List<RadiationReading> findAllSorted() {
        return delegate.findAllSorted();
    }

    @Override
    public List<RadiationReading> findAllWithDeviceAndClient() {
        return delegate.findAllWithDeviceAndClient();
    }

    @Override
    public Double findAverageValue() {
        return delegate.findAverage();
    }

    @Override
    public List<RadiationReading> findByClient(Long clientId) {
        return delegate.findByClient(clientId);
    }

    @Override
    public List<RadiationReading> findByDevice(Long deviceId) {
        return delegate.findByDevice(deviceId);
    }

    @Override
    public List<RadiationReading> findRecent(int limit) {
        return delegate.findRecent(PageRequest.of(0, limit));
    }

    @Override
    public List<RadiationReading> findBySensorId(String serialNumber, int limit) {
        return delegate.findBySensorId(serialNumber, PageRequest.of(0, limit));
    }

    @Override
    public Optional<RadiationReading> findLatestBySensorId(String serialNumber) {
        return delegate.findBySensorId(serialNumber, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Override
    public long countBySensorId(String serialNumber) {
        return delegate.countBySensorId(serialNumber);
    }

    @Override
    public RadiationReading save(RadiationReading reading) {
        return delegate.save(reading);
    }

    @Override
    public List<RadiationReading> saveAll(List<RadiationReading> readings) {
        return delegate.saveAll(readings);
    }

    @Override
    public List<RadiationReading> findAll() {
        return delegate.findAll();
    }
}
