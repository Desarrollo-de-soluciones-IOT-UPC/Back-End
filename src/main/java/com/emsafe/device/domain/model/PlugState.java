package com.emsafe.device.domain.model;

import com.emsafe.shared.domain.exception.BadRequestException;

/**
 * Value Object: estado del relé (enchufe inteligente) de un sensor.
 *
 * <p>Se usa en los dos sentidos del canal de control:
 * <ul>
 *   <li><b>Estado deseado</b> ({@code devices.desired_plug}) — la orden del usuario
 *       desde la app móvil, que el edge consulta en cada ciclo.</li>
 *   <li><b>Estado reportado</b> ({@code radiation_readings.plug}) — lo que el
 *       dispositivo dice que está haciendo realmente.</li>
 * </ul>
 *
 * <p>Nota de negocio (decisión del profesor): la orden del USUARIO manda siempre,
 * incluso en nivel DANGER — el corte automático por umbral no debe pisar un
 * {@code desiredPlug} explícito.
 */
public enum PlugState {

    ON,
    OFF;

    /** Literal exacto que viaja a la BD y al edge ("ON" | "OFF"). */
    public String persistedValue() {
        return name();
    }

    /** Lectura tolerante desde la BD: null o desconocido ⇒ null (sin orden). */
    public static PlugState fromPersisted(String value) {
        return parse(value);
    }

    /** Parseo estricto para la orden que llega de la app móvil. */
    public static PlugState fromApi(String value) {
        PlugState parsed = parse(value);
        if (parsed == null) {
            throw new BadRequestException("Invalid plug state: " + value + " (expected ON or OFF)");
        }
        return parsed;
    }

    private static PlugState parse(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        for (PlugState state : values()) {
            if (state.name().equals(normalized)) {
                return state;
            }
        }
        return null;
    }
}
