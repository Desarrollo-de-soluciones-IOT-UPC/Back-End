package com.emsafe.reporting.application;

import com.emsafe.iam.domain.model.User;
import com.emsafe.monitoring.domain.model.RadiationReading;
import com.emsafe.monitoring.domain.repository.RadiationReadingRepository;
import com.emsafe.reporting.interfaces.rest.dto.ClientRadiationDto;
import com.emsafe.reporting.interfaces.rest.dto.RadiationPointDto;
import com.emsafe.shared.domain.model.RadiationLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapa de radiación del portal de administración, en sus dos vistas: un punto por
 * lectura y un marcador por cliente.
 *
 * <p>Se separa de {@link DashboardQueryService} porque son dos proyecciones distintas
 * sobre datos distintos: el panel agrega cifras de cuatro contextos, el mapa transforma
 * lecturas de Monitoring en marcadores geolocalizados. Juntarlas era lo que hacía del
 * antiguo {@code DashboardService} un cajón de sastre.
 *
 * <p><b>Regla smart-edge:</b> el nivel lo decide el edge y aquí solo se lee
 * ({@code RadiationLevel.of} degrada al valor únicamente si la lectura llegó sin nivel,
 * como las semillas antiguas). El backend no reclasifica.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RadiationMapQueryService {

    private final RadiationReadingRepository readingRepository;

    /** Un punto por lectura, para la capa de puntos del mapa. */
    public List<RadiationPointDto> getRadiationMap() {
        return readingRepository.findAllSorted().stream()
                .map(r -> new RadiationPointDto(
                        r.getId(),
                        r.getLatitude(),
                        r.getLongitude(),
                        r.getLocation(),
                        r.getSensorId(),
                        r.getValue(),
                        RadiationLevel.of(r.getLevel(), r.getValue()).apiValue(),
                        r.getReadingDate() != null ? r.getReadingDate().toString() : null
                ))
                .toList();
    }

    /**
     * Un marcador por cliente, en la sede que tiene registrada, con el desglose de sus
     * sensores. El titular del marcador es el <b>peor sensor ahora mismo</b>: se toma la
     * última lectura de cada dispositivo, no el pico histórico, para que el mapa muestre
     * la situación actual.
     */
    public List<ClientRadiationDto> getRadiationMapByClient() {
        Map<Long, List<RadiationReading>> byClient = readingRepository.findAllWithDeviceAndClient()
                .stream()
                .filter(r -> r.getDevice() != null && r.getDevice().getClient() != null)
                .collect(Collectors.groupingBy(r -> r.getDevice().getClient().getId()));

        List<ClientRadiationDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<RadiationReading>> entry : byClient.entrySet()) {
            List<RadiationReading> clientReadings = entry.getValue();
            User clientUser = clientReadings.get(0).getDevice().getClient();

            List<ClientRadiationDto.DeviceReadingDto> deviceDtos = latestPerDevice(clientReadings);

            double maxVal = deviceDtos.stream()
                    .mapToDouble(ClientRadiationDto.DeviceReadingDto::latestValue)
                    .max().orElse(0.0);
            String level = deviceDtos.stream()
                    .map(ClientRadiationDto.DeviceReadingDto::level)
                    .map(RadiationLevel::fromApi)
                    .reduce(RadiationLevel.SAFE, RadiationLevel::worseOf)
                    .apiValue();

            result.add(new ClientRadiationDto(
                    entry.getKey(),
                    clientUser.getName(),
                    clientUser.getLatitude(),
                    clientUser.getLongitude(),
                    clientUser.getAddress() != null ? clientUser.getAddress() : clientUser.getLocation(),
                    maxVal,
                    level,
                    deviceDtos
            ));
        }

        result.sort(Comparator.comparingDouble(ClientRadiationDto::maxValue).reversed());
        return result;
    }

    /** Última lectura de cada sensor del cliente, de la más alta a la más baja. */
    private List<ClientRadiationDto.DeviceReadingDto> latestPerDevice(List<RadiationReading> readings) {
        return readings.stream()
                .filter(r -> r.getDevice() != null)
                .collect(Collectors.groupingBy(r -> r.getDevice().getId()))
                .values().stream()
                .map(perDevice -> perDevice.stream()
                        .max(Comparator.comparing(RadiationReading::timestamp))
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(latest -> new ClientRadiationDto.DeviceReadingDto(
                        latest.getDevice().getId(),
                        latest.getDevice().getName(),
                        latest.getDevice().getType(),
                        latest.getDevice().getSerialNumber(),
                        latest.getDevice().getLocation(), // zona o sala dentro de la sede
                        latest.getDevice().getStatus().persistedValue(),
                        latest.getValue(),
                        RadiationLevel.of(latest.getLevel(), latest.getValue()).apiValue(),
                        latest.getReadingDate() != null ? latest.getReadingDate().toString() : null
                ))
                .sorted(Comparator.comparingDouble(
                        ClientRadiationDto.DeviceReadingDto::latestValue).reversed())
                .toList();
    }
}
