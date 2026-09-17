package com.emsafe.servicerecord.infrastructure.persistence;

import com.emsafe.servicerecord.domain.model.ServiceRecordStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce {@link ServiceRecordStatus} ⇄ el literal que ya vive en
 * {@code history.status} ("completed" / "cancelled"). Sin migración Flyway.
 */
@Converter(autoApply = true)
public class ServiceRecordStatusConverter
        implements AttributeConverter<ServiceRecordStatus, String> {

    @Override
    public String convertToDatabaseColumn(ServiceRecordStatus status) {
        return (status == null ? ServiceRecordStatus.COMPLETED : status).persistedValue();
    }

    @Override
    public ServiceRecordStatus convertToEntityAttribute(String dbValue) {
        return ServiceRecordStatus.fromPersisted(dbValue);
    }
}
