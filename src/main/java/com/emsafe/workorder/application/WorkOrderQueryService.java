package com.emsafe.workorder.application;

import com.emsafe.device.application.DeviceApplicationService;
import com.emsafe.device.interfaces.rest.dto.DeviceDto;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import com.emsafe.shared.interfaces.rest.PageResponse;
import com.emsafe.workorder.domain.model.WorkOrder;
import com.emsafe.workorder.domain.model.WorkOrderStatus;
import com.emsafe.workorder.domain.model.WorkOrderType;
import com.emsafe.workorder.infrastructure.persistence.SensorRepository;
import com.emsafe.workorder.infrastructure.persistence.WorkOrderRepository;
import com.emsafe.workorder.interfaces.rest.dto.WorkOrderDetailDto;
import com.emsafe.workorder.interfaces.rest.dto.WorkOrderDto;
import com.emsafe.workorder.interfaces.rest.dto.WorkOrderEditDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Lado de LECTURA del contexto WorkOrder (listados, detalle y comprobaciones de acceso).
 *
 * <p>Separarlo del lado de escritura es lo que permite que
 * {@link WorkOrderApplicationService} quede pequeño y centrado en las transiciones de
 * negocio. Aquí no se muta nada.
 */
@Service
@RequiredArgsConstructor
public class WorkOrderQueryService {

    /** Órdenes vivas que se muestran en las listas (Completed/Cancelled solo viven en History). */
    private static final List<WorkOrderStatus> ACTIVE_STATUSES =
            List.of(WorkOrderStatus.PENDING, WorkOrderStatus.IN_PROGRESS);

    private final WorkOrderRepository workOrderRepository;
    private final SensorRepository sensorRepository;
    private final DeviceApplicationService deviceApplicationService;

    // ─── Admin ────────────────────────────────────────────────────────────────

    public PageResponse<WorkOrderDto> findAllPaged(String status, String type, String search,
                                                   String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return PageResponse.of(workOrderRepository
                .searchPaged(resolveStatuses(status), WorkOrderType.parseFilter(type),
                        searchTerm(search), pageable)
                .map(WorkOrderDto::from));
    }

    public List<WorkOrderDto> findAll(String status, String type, String search, String sort) {
        return workOrderRepository
                .search(resolveStatuses(status), WorkOrderType.parseFilter(type),
                        searchTerm(search), buildSort(sort))
                .stream().map(WorkOrderDto::from).toList();
    }

    public WorkOrderEditDto findEditById(Long id) {
        return WorkOrderEditDto.from(require(id));
    }

    public List<WorkOrderDto> findLatestFour() {
        return workOrderRepository.findTop4ByOrderByScheduledDateDesc()
                .stream().map(WorkOrderDto::from).toList();
    }

    public long countByStatus(WorkOrderStatus status) {
        return workOrderRepository.countByStatus(status);
    }

    // ─── Proyecciones para Reporting ──────────────────────────────────────────
    // El dashboard necesita estos conteos; los expone el application service para
    // que Reporting no tenga que alcanzar el repositorio de este contexto.

    /** Sensores registrados en partes de trabajo (entidad interna del agregado). */
    public long countSensors() {
        return sensorRepository.count();
    }

    /** Órdenes cuya ciudad termina en el sufijo dado (", TX", ", CA"...). */
    public long countByCitySuffix(String suffix) {
        return workOrderRepository.countByCitySuffix(suffix);
    }

    // ─── Técnico ──────────────────────────────────────────────────────────────

    public List<WorkOrderDetailDto> findByTechnician(Long technicianId, String status) {
        WorkOrderStatus statusEnum = WorkOrderStatus.parseFilter(status);
        List<WorkOrder> list = statusEnum != null
                ? workOrderRepository.findByTechnicianIdAndStatus(technicianId, statusEnum)
                : workOrderRepository.findByTechnicianId(technicianId);
        return list.stream().map(WorkOrderDetailDto::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkOrderDetailDto findDetailById(Long id) {
        WorkOrder wo = require(id);
        return WorkOrderDetailDto.from(wo, resolveClientDevices(wo));
    }

    /**
     * Control de acceso del portal técnico: la orden debe estar asignada al técnico
     * del JWT (los admin se saltan esto en el controller). La pregunta la responde
     * ahora el propio agregado con {@code isAssignedTo}.
     */
    public void assertOwnedByTechnician(Long orderId, Long technicianId) {
        if (!require(orderId).isAssignedTo(technicianId)) {
            throw new AccessDeniedException("This work order is not assigned to you");
        }
    }

    /** Sensores del cliente de la orden (vacío si no hay cliente enlazado). */
    List<DeviceDto> resolveClientDevices(WorkOrder wo) {
        Long clientId = wo.clientUserId();
        return clientId == null ? List.of() : deviceApplicationService.getByClient(clientId);
    }

    WorkOrder require(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
    }

    // ─── Helpers de filtrado ──────────────────────────────────────────────────

    /**
     * Estados a consultar: el estado activo pedido, o los dos activos cuando no hay
     * filtro (o el filtro es Completed/Cancelled, que solo viven en History). Meter
     * el filtro dentro del IN evita bindear un enum nulo, que Hibernate no sabe tipar.
     */
    private List<WorkOrderStatus> resolveStatuses(String status) {
        WorkOrderStatus parsed = WorkOrderStatus.parseFilter(status);
        return (parsed != null && parsed.isActive()) ? List.of(parsed) : ACTIVE_STATUSES;
    }

    /** 'created' → lo último añadido primero; por defecto → fecha programada más próxima. */
    private Sort buildSort(String sort) {
        return "created".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "id")
                : Sort.by(Sort.Direction.ASC, "scheduledDate");
    }

    private String searchTerm(String search) {
        return StringUtils.hasText(search) ? search : null;
    }
}
