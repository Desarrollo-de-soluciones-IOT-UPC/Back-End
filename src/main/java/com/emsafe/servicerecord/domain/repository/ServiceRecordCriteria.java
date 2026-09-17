package com.emsafe.servicerecord.domain.repository;

import com.emsafe.servicerecord.domain.model.ServiceRecordStatus;

import java.util.List;

/**
 * Criterio de búsqueda del historial, ya normalizado.
 *
 * <p>Existe para que la normalización de los filtros que llegan del portal (cadenas
 * en blanco que significan "sin filtro", el estado en mayúsculas, el término de
 * búsqueda con espacios) se haga <b>una sola vez y en un solo sitio</b>. Antes
 * estaba copiada en los dos métodos de {@code HistoryService}.
 *
 * @param technicianId técnico que firmó el acta; {@code null} = todos (portal admin)
 * @param statuses     desenlaces admitidos; nunca vacío, para no tener que bindear un
 *                     enum nulo en la consulta — Hibernate no sabe tiparlo
 * @param search       término parcial sobre cliente u orderId; {@code null} = sin filtro
 */
public record ServiceRecordCriteria(Long technicianId,
                                    List<ServiceRecordStatus> statuses,
                                    String search) {

    public static ServiceRecordCriteria of(Long technicianId, String status, String search) {
        ServiceRecordStatus parsed = ServiceRecordStatus.parseFilter(status);
        return new ServiceRecordCriteria(
                technicianId,
                parsed != null ? List.of(parsed) : List.of(ServiceRecordStatus.values()),
                (search == null || search.isBlank()) ? null : search.trim());
    }
}
