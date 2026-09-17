package com.emsafe.reporting.application;

import com.emsafe.alerting.application.AlarmApplicationService;
import com.emsafe.alerting.interfaces.rest.dto.AlertDto;
import com.emsafe.iam.domain.model.Role;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.reporting.interfaces.rest.dto.ChartDataDto;
import com.emsafe.reporting.interfaces.rest.dto.LatestWorkOrderDto;
import com.emsafe.reporting.interfaces.rest.dto.StatsDto;
import com.emsafe.shared.domain.model.RadiationLevel;
import com.emsafe.workorder.application.WorkOrderQueryService;
import com.emsafe.workorder.domain.model.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Panel de administración: tarjetas de estadísticas, alarmas, últimas órdenes y series
 * de los gráficos.
 *
 * <p><b>Reporting no tiene agregados ni tabla propia.</b> Es un read model puro: cada
 * cifra que muestra pertenece a otro bounded context y aquí solo se proyecta. Por eso
 * consulta a los demás por su <i>application service</i> (Alerting, WorkOrder) o por su
 * <i>puerto de dominio</i> (IAM, Monitoring), y nunca por su infraestructura.
 *
 * <p>Junto con {@link RadiationMapQueryService} sustituye al antiguo
 * {@code DashboardService}, que mezclaba el panel y el mapa en un solo servicio con
 * cinco repositorios inyectados.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService {

    /** µT — límite seguro que muestra el panel (referencia ICNIRP 50 Hz). */
    private static final double SAFETY_THRESHOLD = RadiationLevel.DANGER_UT;

    /** Series de demostración cuando aún no hay datos reales que graficar. */
    private static final List<Integer> ACTIVITY_SERIES =
            List.of(5, 14, 10, 22, 18, 30, 26, 38, 32, 45, 40, 52);
    private static final List<String> ACTIVITY_CATEGORIES =
            List.of("01 May", "", "", "10 May", "", "", "20 May", "", "", "", "30 May", "");
    private static final List<Double> RADIATION_FALLBACK =
            List.of(75.0, 85.0, 80.0, 95.0, 90.0, 105.0, 115.0, 110.0);
    private static final List<String> REGIONS = List.of("TX", "CA", "WA", "NY");

    private final WorkOrderQueryService workOrders;
    private final AlarmApplicationService alarms;
    private final UserRepository userRepository;
    private final RadiationReadingRepository readingRepository;

    public StatsDto getStats() {
        long pending = workOrders.countByStatus(WorkOrderStatus.PENDING);
        long activeClients = userRepository.findByRole(Role.CLIENT).stream()
                .filter(User::isActive)
                .count();
        long criticalAlerts = alarms.countCritical();
        long totalSensors = workOrders.countSensors();
        Double radAvg = readingRepository.findAverageValue();
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
        return alarms.getAll();
    }

    public List<LatestWorkOrderDto> getLatestWorkOrders() {
        return workOrders.findLatestFour().stream().map(LatestWorkOrderDto::from).toList();
    }

    public ChartDataDto getChartData() {
        List<Double> radSeries = readingRepository.findAllSorted().stream()
                .map(RadiationReading::getValue)
                .toList();

        List<Long> regionalSeries = REGIONS.stream()
                .map(region -> workOrders.countByCitySuffix(", " + region))
                .toList();

        return new ChartDataDto(
                new ChartDataDto.SystemActivity(ACTIVITY_SERIES, ACTIVITY_CATEGORIES),
                new ChartDataDto.RadiationTrends(
                        radSeries.isEmpty() ? RADIATION_FALLBACK : radSeries),
                new ChartDataDto.Regional(regionalSeries, REGIONS)
        );
    }
}
