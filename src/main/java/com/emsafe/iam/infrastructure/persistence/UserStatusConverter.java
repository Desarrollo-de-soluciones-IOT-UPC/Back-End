package com.emsafe.iam.infrastructure.persistence;

import com.emsafe.iam.domain.model.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce {@link UserStatus} ⇄ el literal exacto que ya vive en la columna {@code users.status}.
 *
 * <p>Es la pieza que hace posible la regla R3 del plan: el dominio gana un Value Object
 * sin que la base de datos cambie ni un byte, así que <b>no hace falta migración Flyway</b>
 * y revertir el código revierte todo.
 *
 * <p>{@code autoApply = true} evita que el agregado tenga que importar esta clase de
 * infraestructura: el dominio no sabe que existe.
 */
@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus status) {
        return (status == null ? UserStatus.ACTIVE : status).persistedValue();
    }

    @Override
    public UserStatus convertToEntityAttribute(String dbValue) {
        return UserStatus.fromPersisted(dbValue);
    }
}
