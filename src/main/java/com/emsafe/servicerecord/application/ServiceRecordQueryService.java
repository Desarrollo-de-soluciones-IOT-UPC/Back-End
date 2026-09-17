package com.emsafe.servicerecord.application;

import com.emsafe.servicerecord.domain.repository.ServiceRecordCriteria;
import com.emsafe.servicerecord.domain.repository.ServiceRecordPage;
import com.emsafe.servicerecord.domain.repository.ServiceRecordRepository;
import com.emsafe.servicerecord.interfaces.rest.dto.ServiceRecordDto;
import com.emsafe.shared.interfaces.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lado de LECTURA del contexto ServiceRecord: los dos historiales del portal.
 *
 * <p>La única diferencia entre el historial del admin y el del técnico es que el
 * segundo llega con un {@code technicianId}; el filtrado por técnico es del negocio
 * y por eso vive en el criterio, no repartido en dos consultas distintas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceRecordQueryService {

    private final ServiceRecordRepository repository;

    /**
     * @param technicianId técnico que firmó el acta ({@code null} = todos, portal admin)
     * @param status       "completed" | "cancelled" ({@code null} = todos)
     * @param search       búsqueda parcial por cliente u orderId
     */
    public List<ServiceRecordDto> find(Long technicianId, String status, String search) {
        return repository.search(ServiceRecordCriteria.of(technicianId, status, search))
                .stream().map(ServiceRecordDto::from).toList();
    }

    public PageResponse<ServiceRecordDto> findPaged(Long technicianId, String status,
                                                    String search, int page, int size) {
        ServiceRecordPage found = repository.searchPage(
                ServiceRecordCriteria.of(technicianId, status, search), page, size);
        return new PageResponse<>(
                found.content().stream().map(ServiceRecordDto::from).toList(),
                found.page(), found.size(), found.totalElements(),
                found.totalPages(), found.isLast());
    }
}
