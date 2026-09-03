package com.emsafe.device.infrastructure.persistence;

import com.emsafe.device.domain.model.DeviceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce {@link DeviceStatus} ⇄ el literal que ya vive en {@code devices.status}
 * ("active", "unregistered", "requires-maintenance"...). Sin migración Flyway.
 */
@Converter(autoApply = true)
public class DeviceStatusConverter implements AttributeConverter<DeviceStatus, String> {

    @Override
    public String convertToDatabaseColumn(DeviceStatus status) {
        return (status == null ? DeviceStatus.ACTIVE : status).persistedValue();
    }

    @Override
    public DeviceStatus convertToEntityAttribute(String dbValue) {
        return DeviceStatus.fromPersisted(dbValue);
    }
}
