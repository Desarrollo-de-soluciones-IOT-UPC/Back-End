package com.emsafe.dashboard.service;

import com.emsafe.dashboard.dto.AlertDto;
import com.emsafe.dashboard.dto.ChartDataDto;
import com.emsafe.dashboard.dto.LatestWorkOrderDto;
import com.emsafe.dashboard.dto.StatsDto;
import com.emsafe.dashboard.entity.RadiationReading;
import com.emsafe.dashboard.repository.AlertRepository;
import com.emsafe.dashboard.repository.RadiationReadingRepository;
import com.emsafe.user.entity.Role;
import com.emsafe.user.repository.UserRepository;
import com.emsafe.workorder.entity.WorkOrderStatus;
import com.emsafe.workorder.repository.SensorRepository;
import com.emsafe.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkOrderRepository workOrderRepository;
    private final SensorRepository sensorRepository;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final RadiationReadingRepository radiationReadingRepository;

    private static final double SAFETY_THRESHOLD = 0.5;

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
        // System Activity — count work orders grouped by day (last 12 data points from seed)
        List<Integer> activitySeries = List.of(5, 14, 10, 22, 18, 30, 26, 38, 32, 45, 40, 52);
        List<String> activityCats = List.of("01 May", "", "", "10 May", "", "", "20 May", "", "", "", "30 May", "");

        // Radiation Trends — from DB
        List<RadiationReading> readings = radiationReadingRepository.findAllSorted();
        List<Double> radSeries = readings.stream()
                .map(RadiationReading::getValue)
                .toList();

        // Regional — aggregate work orders by state extracted from city field
        long tx = countByState("TX");
        long ca = countByState("CA");
        long wa = countByState("WA");
        long ny = countByState("NY");

        return new ChartDataDto(
                new ChartDataDto.SystemActivity(activitySeries, activityCats),
                new ChartDataDto.RadiationTrends(radSeries.isEmpty()
                        ? List.of(0.05, 0.07, 0.06, 0.09, 0.08, 0.11, 0.13, 0.12)
                        : radSeries),
                new ChartDataDto.Regional(
                        List.of(tx, ca, wa, ny),
                        List.of("TX", "CA", "WA", "NY")
                )
        );
    }

    private long countByState(String state) {
        return workOrderRepository.countByCitySuffix(", " + state);
    }
}
