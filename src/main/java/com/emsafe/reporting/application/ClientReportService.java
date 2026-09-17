package com.emsafe.reporting.application;

import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.reporting.interfaces.rest.dto.ClientReportDto;
import com.emsafe.shared.domain.model.RadiationLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Informe mensual/anual de exposición de un cliente.
 *
 * <p>Vivía dentro de {@code ClientService}, en el portal móvil. Pero agrupar lecturas en
 * cubos, promediar, sacar picos y contar alertas <b>es reporting</b>, no presentación: el
 * día que la web quiera el mismo informe no habría que duplicarlo. Aquí queda junto al
 * resto de proyecciones, y el BFF se limita a exponerlo.
 *
 * <p>Las alertas se cuentan con {@code effectiveLevel()} del agregado — la regla
 * smart-edge — en vez de comparar contra la cadena {@code "safe"} a mano.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientReportService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");

    private final RadiationReadingRepository readingRepository;

    /**
     * @param period "year" agrupa los últimos 12 meses por mes; cualquier otro valor
     *               agrupa los últimos 30 días por día
     */
    public ClientReportDto forClient(Long clientId, String period) {
        boolean yearly = "year".equalsIgnoreCase(period);
        LocalDate from = yearly
                ? LocalDate.now().minusMonths(11).withDayOfMonth(1)
                : LocalDate.now().minusDays(29);

        List<RadiationReading> readings = readingRepository.findByClient(clientId).stream()
                .filter(r -> r.getReadingDate() != null && r.getValue() != null
                        && !r.getReadingDate().isBefore(from))
                .toList();

        return new ClientReportDto(
                yearly ? "year" : "month",
                round3(average(readings)),
                round3(peak(readings)),
                readings.size(),
                countAlerts(readings),
                bucketize(readings, yearly));
    }

    /** Agrupa en cubos de día o de mes, del más antiguo al más reciente. */
    private List<ClientReportDto.Bucket> bucketize(List<RadiationReading> readings, boolean yearly) {
        Map<String, List<RadiationReading>> grouped = new LinkedHashMap<>();
        readings.stream()
                .sorted(Comparator.comparing(RadiationReading::getReadingDate))
                .forEach(r -> grouped
                        .computeIfAbsent(bucketLabel(r, yearly), k -> new ArrayList<>())
                        .add(r));

        return grouped.entrySet().stream()
                .map(e -> new ClientReportDto.Bucket(
                        e.getKey(),
                        round3(average(e.getValue())),
                        round3(peak(e.getValue())),
                        e.getValue().size(),
                        countAlerts(e.getValue())))
                .toList();
    }

    private String bucketLabel(RadiationReading r, boolean yearly) {
        return yearly
                ? YearMonth.from(r.getReadingDate()).format(MONTH_LABEL)
                : r.getReadingDate().format(DAY_LABEL);
    }

    private double average(List<RadiationReading> readings) {
        return readings.stream().mapToDouble(RadiationReading::getValue).average().orElse(0);
    }

    private double peak(List<RadiationReading> readings) {
        return readings.stream().mapToDouble(RadiationReading::getValue).max().orElse(0);
    }

    /** Una alerta es toda lectura que el edge no clasificó como segura. */
    private int countAlerts(List<RadiationReading> readings) {
        return (int) readings.stream()
                .filter(r -> r.effectiveLevel() != RadiationLevel.SAFE)
                .count();
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
