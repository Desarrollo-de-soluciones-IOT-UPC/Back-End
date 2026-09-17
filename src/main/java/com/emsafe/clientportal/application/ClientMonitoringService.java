package com.emsafe.clientportal.application;

import com.emsafe.clientportal.interfaces.rest.dto.ClientAlertDto;
import com.emsafe.clientportal.interfaces.rest.dto.ClientDashboardDto;
import com.emsafe.clientportal.interfaces.rest.dto.ClientDeviceDto;
import com.emsafe.clientportal.interfaces.rest.dto.ClientReadingDto;
import com.emsafe.device.domain.model.Device;
import com.emsafe.device.domain.model.DeviceStatus;
import com.emsafe.device.domain.repository.DeviceRepository;
import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.shared.domain.model.RadiationLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Lecturas, panel y alertas del cliente autenticado.
 *
 * <p>Es proyección pura: no muta nada y no decide niveles. El nivel de cada lectura lo
 * responde el agregado con {@code levelForApi()} — la regla smart-edge — en vez de
 * recalcularse aquí, que era lo que hacía el {@code levelOf()} privado del antiguo
 * {@code ClientService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientMonitoringService {

    /** Umbral (µT) a partir del cual se considera superado el límite seguro. */
    private static final double SAFETY_THRESHOLD = RadiationLevel.DANGER_UT;

    /** Lecturas del panel: las N más recientes que se listan bajo el resumen. */
    private static final int DASHBOARD_LATEST_READINGS = 10;

    private final DeviceRepository deviceRepository;
    private final RadiationReadingRepository readingRepository;

    public List<ClientReadingDto> getReadings(Long clientId) {
        return readingRepository.findByClient(clientId).stream()
                .map(ClientReadingDto::from)
                .toList();
    }

    /** Alertas = toda lectura del cliente que el edge no clasificó como segura. */
    public List<ClientAlertDto> getAlerts(Long clientId) {
        return readingRepository.findByClient(clientId).stream()
                .filter(r -> r.effectiveLevel() != RadiationLevel.SAFE)
                .sorted(Comparator.comparing(RadiationReading::timestamp).reversed())
                .map(ClientAlertDto::from)
                .toList();
    }

    public ClientDashboardDto getDashboard(Long clientId) {
        List<Device> devices = deviceRepository.findByClient(clientId);
        List<RadiationReading> readings = readingRepository.findByClient(clientId);

        List<ClientDeviceDto> deviceDtos = devices.stream()
                .map(d -> ClientDeviceDto.from(d, readings.stream()
                        .filter(r -> r.getDevice() != null && r.getDevice().getId().equals(d.getId()))
                        .toList()))
                .toList();

        return new ClientDashboardDto(
                devices.size(),
                (int) devices.stream().filter(d -> d.getStatus() == DeviceStatus.ACTIVE).count(),
                round3(recentAverage(readings)),
                round3(readings.stream().mapToDouble(RadiationReading::getValue).max().orElse(0.0)),
                worstLevelOf(deviceDtos),
                SAFETY_THRESHOLD,
                (int) readings.stream()
                        .filter(r -> r.effectiveLevel() != RadiationLevel.SAFE)
                        .count(),
                deviceDtos,
                // las lecturas ya vienen ordenadas por recordedAt descendente
                readings.stream()
                        .limit(DASHBOARD_LATEST_READINGS)
                        .map(ClientReadingDto::from)
                        .toList()
        );
    }

    /**
     * Media de las últimas 24 h, no de toda la historia.
     *
     * <p>Una media histórica se queda alta para siempre después de un solo pico, y en la
     * pantalla se lee como "media alta pero nivel verde", que es contradictorio.
     */
    private double recentAverage(List<RadiationReading> readings) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        return readings.stream()
                .filter(r -> r.timestamp().isAfter(cutoff))
                .mapToDouble(RadiationReading::getValue)
                .average().orElse(0.0);
    }

    /** Nivel global = el peor nivel actual entre los sensores del cliente. */
    private String worstLevelOf(List<ClientDeviceDto> devices) {
        return devices.stream()
                .map(ClientDeviceDto::latestLevel)
                .filter(java.util.Objects::nonNull)
                .map(RadiationLevel::fromApi)
                .reduce(RadiationLevel.SAFE, RadiationLevel::worseOf)
                .apiValue();
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
