package com.emsafe.monitoring.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Representación de una lectura para consumir desde web/móvil.
 * Mantiene la clave `field_uT` para ser consistente con el payload del edge.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingDto {

    private Long id;

    /** Serial físico del sensor (lo que manda el edge). */
    private String serialNumber;

    /** id numérico autogenerado del Device en BD (para enlazar en el front). */
    private Long deviceDbId;

    private String deviceName;

    @JsonProperty("field_uT")
    private Double fieldUT;

    private String level;

    private String message;

    /** Estado del relé reportado por el dispositivo en esta lectura (ON | OFF). */
    private String plug;

    private String location;

    private Double latitude;

    private Double longitude;

    private Long clientId;

    private String clientName;

    private LocalDateTime recordedAt;

    /**
     * Proyecta el agregado a su representación de API.
     *
     * <p>Ojo con {@code level}: se devuelve el valor <b>verbatim del edge</b>
     * (en MAYÚSCULA), que es lo que el front y la app móvil ya consumen. El nivel
     * normalizado en minúscula se obtiene con {@code reading.levelForApi()} y lo
     * usan los DTOs del portal de cliente y del mapa.
     */
    public static ReadingDto from(com.emsafe.monitoring.domain.model.RadiationReading r) {
        com.emsafe.device.domain.model.Device d = r.getDevice();
        com.emsafe.iam.domain.model.User client = r.owner();
        return ReadingDto.builder()
                .id(r.getId())
                .serialNumber(r.getSensorId())
                .deviceDbId(d != null ? d.getId() : null)
                .deviceName(d != null ? d.getName() : null)
                .fieldUT(r.getValue())
                .level(r.getLevel())
                .message(r.getMessage())
                .plug(r.getPlug())
                .location(r.getLocation())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .clientId(client != null ? client.getId() : null)
                .clientName(client != null ? client.getName() : null)
                .recordedAt(r.getRecordedAt())
                .build();
    }
}
