package com.emsafe.alerting.infrastructure.persistence;

import com.emsafe.alerting.domain.model.AlertType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Traduce {@link AlertType} ⇄ el literal de {@code alerts.type}. Sin migración Flyway. */
@Converter(autoApply = true)
public class AlertTypeConverter implements AttributeConverter<AlertType, String> {

    @Override
    public String convertToDatabaseColumn(AlertType type) {
        return (type == null ? AlertType.INFO : type).persistedValue();
    }

    @Override
    public AlertType convertToEntityAttribute(String dbValue) {
        return AlertType.fromPersisted(dbValue);
    }
}
