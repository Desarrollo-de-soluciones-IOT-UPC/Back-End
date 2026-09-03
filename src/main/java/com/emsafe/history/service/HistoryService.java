package com.emsafe.history.service;

import com.emsafe.history.dto.HistoryDto;
import com.emsafe.history.entity.History;
import com.emsafe.history.repository.HistoryRepository;
import com.emsafe.workorder.domain.model.WorkOrder;
import com.emsafe.workorder.infrastructure.persistence.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final HistoryRepository historyRepository;
    private final WorkOrderRepository workOrderRepository;

    /**
     * Escribe el acta de servicio de una orden que acaba de cerrarse.
     *
     * <p>Lo invoca el suscriptor de {@code WorkOrderCompleted}/{@code WorkOrderCancelled}.
     * El formato de la hora y la representación del tipo de servicio son conocimiento
     * de ESTE contexto; antes vivían dentro de {@code WorkOrderService}.
     */
    @Transactional
    public void recordFromWorkOrder(Long workOrderId, String status) {
        WorkOrder wo = workOrderRepository.findById(workOrderId).orElse(null);
        if (wo == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        historyRepository.save(History.builder()
                .orderId(wo.getOrderId())
                .completionDate(now.toLocalDate())
                .completionTime(now.format(TIME_FMT))
                .client(wo.getClient())
                .site(wo.getLocation())
                .serviceType(wo.getType().displayValue())
                .technician(wo.getTechnicianName())
                .technicianInitials(wo.getTechnicianInitials())
                .status(status)
                .technicianId(wo.technicianId())
                .workOrderId(wo.getId())
                .build());
    }

    /**
     * @param technicianId filter by technician (null = all, for admin portal)
     * @param status       filter by status (null = all)
     * @param search       partial search on client or orderId
     */
    public com.emsafe.shared.interfaces.rest.PageResponse<HistoryDto> findAllPaged(Long technicianId, String status, String search, int page, int size) {
        String statusFilter = StringUtils.hasText(status) ? status.toLowerCase() : null;
        String searchFilter = StringUtils.hasText(search) ? search : null;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return com.emsafe.shared.interfaces.rest.PageResponse.of(
                historyRepository.searchPaged(technicianId, statusFilter, searchFilter, pageable)
                        .map(HistoryDto::from)
        );
    }

    public List<HistoryDto> findAll(Long technicianId, String status, String search) {
        String statusFilter = StringUtils.hasText(status) ? status.toLowerCase() : null;
        String searchFilter = StringUtils.hasText(search) ? search : null;

        return historyRepository.search(technicianId, statusFilter, searchFilter)
                .stream().map(HistoryDto::from).toList();
    }
}
