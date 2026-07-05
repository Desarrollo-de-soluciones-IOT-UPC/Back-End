package com.emsafe.dashboard.service;

import com.emsafe.dashboard.dto.AlertDto;
import com.emsafe.dashboard.dto.ChartDataDto;
import com.emsafe.dashboard.dto.ClientRadiationDto;
import com.emsafe.dashboard.dto.LatestWorkOrderDto;
import com.emsafe.dashboard.dto.RadiationPointDto;
import com.emsafe.dashboard.dto.StatsDto;
import com.emsafe.dashboard.entity.RadiationReading;
import com.emsafe.dashboard.repository.AlertRepository;
import com.emsafe.dashboard.repository.RadiationReadingRepository;
import com.emsafe.shared.RadiationLevel;
import com.emsafe.user.entity.Role;
import com.emsafe.user.repository.UserRepository;
import com.emsafe.workorder.entity.WorkOrderStatus;
import com.emsafe.workorder.repository.SensorRepository;
import com.emsafe.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkOrderRepository workOrderRepository;
    private final SensorRepository sensorRepository;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final RadiationReadingRepository radiationReadingRepository;

    /** µT — safe limit shown in the admin dashboard (ICNIRP 50 Hz reference). */
    private static final double SAFETY_THRESHOLD = RadiationLevel.DANGER_UT;

    public StatsDto getStats() {
        long pending = workOrderRepository.countByStatus(WorkOrderStatus.PENDING);
        long activeClients = userRepository.findByRole(Role.CLIENT).stream()
                .filter(u -> "active".equalsIgnoreCase(u.getStatus()))
                .count();
        long criticalAlerts = alertRepository.countByType("danger");
        long totalSensors = sensorRepository.count();
        Double radAvg = radiationReadingRepository.findAverage();
        double currentRadAvg = radAvg != null ? Math.round(radAvg * 1000.0) / 1000.0 : 0.0;

        return new StatsDto(
                totalSensors > 0 ? totalSensors : 1284,
                "+12%",
                activeClients,
                "+3",
                pending,
                "Stable",
                criticalAlerts,
                criticalAlerts > 1 ? "High Risk" : "Normal",
                4.2,
                currentRadAvg,
                SAFETY_THRESHOLD
        );
    }

    public List<AlertDto> getAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(AlertDto::from).toList();
    }

    public List<LatestWorkOrderDto> getLatestWorkOrders() {
        return workOrderRepository.findTop4ByOrderByScheduledDateDesc()
                .stream().map(LatestWorkOrderDto::from).toList();
    }

    public ChartDataDto getChartData() {
        List<Integer> activitySeries = List.of(5, 14, 10, 22, 18, 30, 26, 38, 32, 45, 40, 52);
        List<String> activityCats = List.of("01 May", "", "", "10 May", "", "", "20 May", "", "", "", "30 May", "");

        List<RadiationReading> readings = radiationReadingRepository.findAllSorted();
        List<Double> radSeries = readings.stream()
                .map(RadiationReading::getValue)
                .toList();

        long tx = countByState("TX");
        long ca = countByState("CA");
        long wa = countByState("WA");
        long ny = countByState("NY");

        return new ChartDataDto(
                new ChartDataDto.SystemActivity(activitySeries, activityCats),
                new ChartDataDto.RadiationTrends(radSeries.isEmpty()
                        ? List.of(75.0, 85.0, 80.0, 95.0, 90.0, 105.0, 115.0, 110.0)
                        : radSeries),
                new ChartDataDto.Regional(
                        List.of(tx, ca, wa, ny),
                        List.of("TX", "CA", "WA", "NY")
                )
        );
    }

    public List<RadiationPointDto> getRadiationMap() {
        return radiationReadingRepository.findAllSorted().stream()
                .map(r -> new RadiationPointDto(
                        r.getId(),
                        r.getLatitude(),
                        r.getLongitude(),
                        r.getLocation(),
                        r.getSensorId(),
                        r.getValue(),
                        RadiationLevel.of(r),
                        r.getReadingDate() != null ? r.getReadingDate().toString() : null
                ))
                .toList();
    }

    /**
     * Groups radiation readings by their client user (via device → client).
     * Returns one marker per client at the location of their highest reading.
     */
    public List<ClientRadiationDto> getRadiationMapByClient() {
        List<RadiationReading> readings = radiationReadingRepository.findAllWithDeviceAndClient();

        // Group readings by client; skip readings not linked to a device with a client
        Map<Long, List<RadiationReading>> byClient = readings.stream()
                .filter(r -> r.getDevice() != null && r.getDevice().getClient() != null)
                .collect(Collectors.groupingBy(r -> r.getDevice().getClient().getId()));

        List<ClientRadiationDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<RadiationReading>> entry : byClient.entrySet()) {
            Long clientId = entry.getKey();
            List<RadiationReading> clientReadings = entry.getValue();

            // Get the client entity (same for all readings in this group)
            var clientUser = clientReadings.get(0).getDevice().getClient();

            // Max reading is shown as the peak; the marker color comes from the
            // worst edge-computed level among this client's readings (smart edge).
            double maxVal = clientReadings.stream()
                    .mapToDouble(RadiationReading::getValue)
                    .max().orElse(0.0);
            String level = clientReadings.stream()
                    .map(RadiationLevel::of)
                    .reduce("safe", RadiationLevel::worse);

            // Marker uses the CLIENT'S registered lat/lng (their physical site)
            Double lat = clientUser.getLatitude();
            Double lng = clientUser.getLongitude();
            String address = clientUser.getAddress() != null ? clientUser.getAddress() : clientUser.getLocation();

            // One entry per device: highest reading for that device
            Map<Long, List<RadiationReading>> byDevice = clientReadings.stream()
                    .filter(r -> r.getDevice() != null)
                    .collect(Collectors.groupingBy(r -> r.getDevice().getId()));

            List<ClientRadiationDto.DeviceReadingDto> deviceDtos = byDevice.entrySet().stream()
                    .map(de -> {
                        RadiationReading best = de.getValue().stream()
                                .max(Comparator.comparingDouble(RadiationReading::getValue))
                                .orElse(null);
                        if (best == null) return null;
                        return new ClientRadiationDto.DeviceReadingDto(
                                best.getDevice().getId(),
                                best.getDevice().getName(),
                                best.getDevice().getType(),
                                best.getDevice().getSerialNumber(),
                                best.getDevice().getLocation(), // zone/room within the facility
                                best.getDevice().getStatus(),
                                best.getValue(),
                                RadiationLevel.of(best),
                                best.getReadingDate() != null ? best.getReadingDate().toString() : null
                        );
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingDouble(ClientRadiationDto.DeviceReadingDto::latestValue).reversed())
                    .toList();

            result.add(new ClientRadiationDto(
                    clientId,
                    clientUser.getName(),
                    lat,
                    lng,
                    address,
                    maxVal,
                    level,
                    deviceDtos
            ));
        }

        result.sort(Comparator.comparingDouble(ClientRadiationDto::maxValue).reversed());
        return result;
    }

    private long countByState(String state) {
        return workOrderRepository.countByCitySuffix(", " + state);
    }
}
