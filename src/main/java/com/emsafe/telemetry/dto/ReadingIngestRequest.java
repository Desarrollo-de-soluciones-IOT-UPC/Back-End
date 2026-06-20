package com.emsafe.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload que envía el edge app en cada medición:
 * <pre>
 * { "serialNumber": "...", "field_uT": 123.4, "level": "MODERADO",
 *   "message": "Precaución: exposición moderada" }
 * </pre>
 */
@Getter
@Setter
public class ReadingIngestRequest {

    /** Serial físico del sensor (= Device.serialNumber). Llave de cruce con el device. */
    private String serialNumber;

    /** Campo magnético en microtesla. Se almacena en RadiationReading.value. */
    @JsonProperty("field_uT")
    private Double fieldUT;

    /** Nivel reportado (e.g. SEGURO | MODERADO | ALTO). */
    private String level;

    /** Mensaje descriptivo de la lectura. */
    private String message;
}
