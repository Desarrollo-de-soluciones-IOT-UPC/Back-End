package com.emsafe.clientportal.interfaces.rest.dto;

import java.util.List;

/**
 * Resumen agregado para el panel de la app móvil.
 */
public record ClientDashboardDto(
        int deviceCount,
        int activeDeviceCount,
        double currentAverage,
        double maxValue,
        String level,
        double safetyThreshold,
        int alertCount,
        List<ClientDeviceDto> devices,
        List<ClientReadingDto> latestReadings
) {}
