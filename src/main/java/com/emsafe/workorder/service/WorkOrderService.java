package com.emsafe.workorder.service;

import com.emsafe.shared.exception.BadRequestException;
import com.emsafe.shared.exception.ResourceNotFoundException;
import com.emsafe.user.entity.AppUser;
import com.emsafe.user.repository.UserRepository;
import com.emsafe.workorder.dto.*;
import com.emsafe.workorder.entity.*;
import com.emsafe.workorder.repository.WorkOrderRepository;
import com.emsafe.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    // ─── Admin endpoints ───────────────────────────────────────────────────────

    public PageResponse<WorkOrderDto> findAllPaged(String status, String type, String search, int page, int size) {
        WorkOrderStatus statusEnum = parseStatus(status);
        WorkOrderType typeEnum = parseType(type);
        String searchTerm = StringUtils.hasText(search) ? search : null;
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(workOrderRepository.searchPaged(statusEnum, typeEnum, searchTerm, pageable)
                .map(WorkOrderDto::from));
    }

    public List<WorkOrderDto> findAll(String status, String type, String search) {
        WorkOrderStatus statusEnum = parseStatus(status);
        WorkOrderType typeEnum = parseType(type);
        String searchTerm = StringUtils.hasText(search) ? search : null;

        return workOrderRepository.search(statusEnum, typeEnum, searchTerm)
                .stream().map(WorkOrderDto::from).toList();
    }

    @Transactional
    public WorkOrderDto create(CreateWorkOrderRequest req) {
        WorkOrder wo = WorkOrder.builder()
                .type(req.type())
                .client(req.client())
                .location(req.location())
                .city(req.city())
                .scheduledDate(req.scheduledDate())
                .scheduledTime(req.scheduledTime())
                .priority(req.priority())
                .contactName(req.contactName())
                .contactRole(req.contactRole())
                .contactPhone(req.contactPhone())
                .contactEmail(req.contactEmail())
                .accessInstructions(req.accessInstructions())
                .expectedSensors(req.expectedSensors())
                .assetId(req.assetId())
                .status(WorkOrderStatus.PENDING)
                .build();

        if (req.requiredTools() != null) {
            wo.getRequiredTools().addAll(req.requiredTools());
        }

        if (req.technicianId() != null) {
            AppUser technician = userRepository.findById(req.technicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technician", req.technicianId()));
            wo.setTechnician(technician);
            wo.setTechnicianName(technician.getName());
            wo.setTechnicianInitials(technician.getInitials());
        }

        WorkOrder saved = workOrderRepository.save(wo);

        // Persist auto-generated orderId
        if (saved.getOrderId() == null) {
            saved.setOrderId(String.format("#WO-%04d", saved.getId()));
            workOrderRepository.save(saved);
        }

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!workOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("WorkOrder", id);
        }
        workOrderRepository.deleteById(id);
    }

    // ─── Tech endpoints ────────────────────────────────────────────────────────

    public List<WorkOrderDetailDto> findByTechnician(Long technicianId, String status) {
        List<WorkOrder> list;
        WorkOrderStatus statusEnum = parseStatus(status);

        if (statusEnum != null) {
            list = workOrderRepository.findByTechnicianIdAndStatus(technicianId, statusEnum);
        } else {
            list = workOrderRepository.findByTechnicianId(technicianId);
        }
        return list.stream().map(WorkOrderDetailDto::from).toList();
    }

    public WorkOrderDetailDto findDetailById(Long id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
        return WorkOrderDetailDto.from(wo);
    }

    @Transactional
    public WorkOrderDetailDto patch(Long id, PatchWorkOrderRequest req) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));

        if (StringUtils.hasText(req.status())) {
            wo.setStatus(parseStatusStrict(req.status()));
        }

        if (StringUtils.hasText(req.technicianNotes())) {
            wo.setTechnicianNotes(req.technicianNotes());
        }

        if (req.sensors() != null && !req.sensors().isEmpty()) {
            req.sensors().forEach(s -> {
                // Update existing sensor by sensorId if found; otherwise add new one
                wo.getSensors().stream()
                        .filter(existing -> existing.getSensorId().equals(s.sensorId()))
                        .findFirst()
                        .ifPresentOrElse(
                                existing -> existing.setStatus(s.status()),
                                () -> wo.getSensors().add(
                                        Sensor.builder()
                                                .sensorId(s.sensorId())
                                                .location(s.location())
                                                .status(s.status())
                                                .workOrder(wo)
                                                .build()
                                )
                        );
            });
        }

        if (req.activityLogEntry() != null) {
            ActivityLogEntry entry = ActivityLogEntry.builder()
                    .event(req.activityLogEntry().event())
                    .logTime(req.activityLogEntry().time())
                    .workOrder(wo)
                    .build();
            wo.getActivityLog().add(entry);
        }

        return WorkOrderDetailDto.from(workOrderRepository.save(wo));
    }

    // ─── Dashboard helpers ─────────────────────────────────────────────────────

    public List<WorkOrderDto> findLatestFour() {
        return workOrderRepository.findTop4ByOrderByScheduledDateDesc()
                .stream().map(WorkOrderDto::from).toList();
    }

    public long countByStatus(WorkOrderStatus status) {
        return workOrderRepository.countByStatus(status);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private WorkOrderStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return WorkOrderStatus.valueOf(status.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }

    private WorkOrderStatus parseStatusStrict(String status) {
        try {
            return WorkOrderStatus.valueOf(status.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }

    private WorkOrderType parseType(String type) {
        if (!StringUtils.hasText(type)) return null;
        try {
            return WorkOrderType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid type: " + type);
        }
    }
}
