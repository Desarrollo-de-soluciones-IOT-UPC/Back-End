package com.emsafe.servicerecord.domain.repository;

import com.emsafe.servicerecord.domain.model.ServiceRecord;

import java.util.List;

/**
 * Una página de actas, expresada sin Spring.
 *
 * <p>Es lo que permite que {@link ServiceRecordRepository} siga siendo un puerto de
 * dominio limpio: {@code Pageable} y {@code Page} quedan confinados al adaptador,
 * igual que en el puerto de Monitoring.
 */
public record ServiceRecordPage(List<ServiceRecord> content, int page, int size,
                                long totalElements) {

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean isLast() {
        return page >= totalPages() - 1;
    }
}
