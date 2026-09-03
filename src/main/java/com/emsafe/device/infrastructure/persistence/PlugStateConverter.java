package com.emsafe.device.infrastructure.persistence;

import com.emsafe.device.domain.model.PlugState;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce {@link PlugState} ⇄ {@code devices.desired_plug} ("ON" | "OFF" | NULL).
 *
 * <p>El null es significativo: quiere decir "el usuario no ha dado ninguna orden",
 * y el edge lo interpreta como "haz lo que decida el dispositivo".
 */
@Converter(autoApply = true)
public class PlugStateConverter implements AttributeConverter<PlugState, String> {

    @Override
    public String convertToDatabaseColumn(PlugState state) {
        return state == null ? null : state.persistedValue();
    }

    @Override
    public PlugState convertToEntityAttribute(String dbValue) {
        return PlugState.fromPersisted(dbValue);
    }
}
