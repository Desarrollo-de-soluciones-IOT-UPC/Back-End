package com.emsafe.reporting.interfaces.rest.dto;

import java.util.List;

/**
 * Informe agregado de radiación para la app móvil (US19/US20/US22 + TS07).
 *
 * period = "month" → últimos 30 días agrupados por día.
 * period = "year"  → últimos 12 meses agrupados por mes.
 */
public record ClientReportDto(
        String period,
        double average,
        double peak,
        int totalReadings,
        int totalAlerts,
        List<Bucket> buckets
) {
    /** Un cubo de agregación (un día o un mes), del más antiguo al más reciente. */
    public record Bucket(
            String label,
            double average,
            double peak,
            int readings,
            int alerts
    ) {}
}
