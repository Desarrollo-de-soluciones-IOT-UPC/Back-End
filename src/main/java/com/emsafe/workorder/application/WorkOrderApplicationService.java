package com.emsafe.workorder.application;

import com.emsafe.device.application.DeviceApplicationService;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.shared.domain.event.DomainEventPublisher;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import com.emsafe.workorder.domain.event.MaintenanceStarted;
import com.emsafe.workorder.domain.event.WorkOrderCancelled;
import com.emsafe.workorder.domain.event.WorkOrderCompleted;
import com.emsafe.workorder.domain.event.WorkOrderCreated;
import com.emsafe.workorder.domain.model.WorkOrder;
import com.emsafe.workorder.domain.model.WorkOrderStatus;
import com.emsafe.workorder.infrastructure.persistence.WorkOrderRepository;
import com.emsafe.workorder.interfaces.rest.dto.CreateWorkOrderRequest;
import com.emsafe.workorder.interfaces.rest.dto.PatchWorkOrderRequest;
import com.emsafe.workorder.interfaces.rest.dto.UpdateWorkOrderRequest;
import com.emsafe.workorder.interfaces.rest.dto.WorkOrderDetailDto;
import com.emsafe.workorder.interfaces.rest.dto.WorkOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Lado de ESCRITURA del contexto WorkOrder: los casos de uso que cambian el estado.
 *
 * <p>Sustituye al antiguo {@code WorkOrderService} de 512 líneas que inyectaba cinco
 * repositorios de cuatro contextos ({@code WorkOrder}, {@code User}, {@code Device},
 * {@code Alert}, {@code History}). Ahora:
 * <ul>
 *   <li>Solo persiste su propio agregado ({@code WorkOrderRepository}).</li>
 *   <li>Habla con Device a través de su <b>application service</b>, nunca de su repositorio.</li>
 *   <li>No conoce Alerting ni History: publica eventos y ellos reaccionan.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class WorkOrderApplicationService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderQueryService queryService;
    private final UserRepository userRepository;
    private final DeviceApplicationService deviceApplicationService;
    private final DomainEventPublisher events;

    // ─── Admin ────────────────────────────────────────────────────────────────

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
                .technicianNotes(req.notes())   // notas del admin, visibles para el técnico
                .status(WorkOrderStatus.PENDING)
                .build();

        if (req.requiredTools() != null) {
            wo.getRequiredTools().addAll(req.requiredTools());
        }
        if (req.clientId() != null && req.clientId() != 0) {
            wo.linkClient(userRepository.findById(req.clientId()).orElse(null));
        }
        if (req.technicianId() != null) {
            wo.assignTechnician(requireTechnician(req.technicianId()));
        }

        WorkOrder saved = workOrderRepository.save(wo);
        // El código legible solo puede calcularse cuando la BD ya asignó el id.
        saved.assignOrderId();
        saved = workOrderRepository.save(saved);

        events.publish(new WorkOrderCreated(saved.getId(), saved.getOrderId(),
                saved.getClient(), saved.getTechnicianName(), saved.clientUserId()));

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public WorkOrderDto update(Long id, UpdateWorkOrderRequest req) {
        WorkOrder wo = queryService.require(id);

        if (req.type() != null) wo.setType(req.type());
        if (StringUtils.hasText(req.client())) wo.setClient(req.client());
        if (req.location() != null) wo.setLocation(req.location());
        if (req.city() != null) wo.setCity(req.city());
        wo.reschedule(req.scheduledDate(), req.scheduledTime());
        if (StringUtils.hasText(req.priority())) wo.setPriority(req.priority());
        if (req.notes() != null) wo.setTechnicianNotes(req.notes());

        if (req.clientId() != null) {
            wo.linkClient(req.clientId() == 0 ? null
                    : userRepository.findById(req.clientId()).orElse(null));
        }
        if (req.technicianId() != null) {
            if (req.technicianId() == 0) {
                wo.unassignTechnician();
            } else {
                wo.assignTechnician(requireTechnician(req.technicianId()));
            }
        }

        return WorkOrderDto.from(workOrderRepository.save(wo));
    }

    /**
     * "Eliminar" desde el admin = cancelar. La orden se conserva (soft-cancel) para
     * que su detalle siga disponible desde el historial; las listas activas la excluyen.
     */
    @Transactional
    public void delete(Long id, String reason) {
        WorkOrder wo = queryService.require(id);
        wo.cancel(reason);
        WorkOrder saved = workOrderRepository.save(wo);
        events.publish(new WorkOrderCancelled(saved.getId(), saved.getOrderId(), reason));
    }

    // ─── Parte del técnico ────────────────────────────────────────────────────

    /**
     * Aplica el parte de campo: cambio de estado, notas, sensores, evidencias,
     * acciones de mantenimiento y altas/bajas de dispositivos.
     */
    @Transactional
    public WorkOrderDetailDto patch(Long id, PatchWorkOrderRequest req) {
        WorkOrder wo = queryService.require(id);
        WorkOrderStatus previousStatus = wo.getStatus();

        if (StringUtils.hasText(req.status())) {
            applyStatusChange(wo, previousStatus, req);
        }

        wo.appendNotes(req.technicianNotes());

        if (req.sensors() != null) {
            req.sensors().forEach(s -> wo.recordSensor(s.sensorId(), s.location(), s.status()));
        }

        // Instalación (descubrimiento): el técnico reclama sensores ya vistos por el edge.
        if (req.claimedDevices() != null) {
            for (PatchWorkOrderRequest.ClaimDeviceDto c : req.claimedDevices()) {
                deviceApplicationService.claimForInstallation(
                        c.deviceId(), c.serialNumber(), c.name(), c.type(),
                        wo.getClientUser(), wo.getLocation());
            }
        }
        // Instalación (legacy): alta a partir de un serial tecleado.
        if (req.newDevices() != null) {
            for (PatchWorkOrderRequest.NewDeviceDto d : req.newDevices()) {
                deviceApplicationService.registerInstalled(
                        d.serialNumber(), d.name(), d.type(), wo.getLocation(), wo.getClientUser());
            }
        }
        // Mantenimiento / recolección: cambio de estado de los sensores.
        if (req.deviceUpdates() != null) {
            for (PatchWorkOrderRequest.DeviceStatusDto u : req.deviceUpdates()) {
                deviceApplicationService.updateStatusFromField(u.deviceId(), u.status());
            }
        }
        if (req.maintenanceActions() != null) {
            for (PatchWorkOrderRequest.MaintenanceActionPatchDto a : req.maintenanceActions()) {
                wo.recordMaintenanceAction(a.deviceId(), a.deviceName(), a.action(), a.description());
            }
        }
        if (req.evidence() != null) {
            req.evidence().forEach(wo::attachEvidence);
        }
        if (req.activityLogEntry() != null) {
            wo.logActivity(req.activityLogEntry().event(), req.activityLogEntry().time());
        }

        WorkOrder saved = workOrderRepository.save(wo);

        // El acta de servicio se escribe en la PRIMERA transición a COMPLETED.
        if (previousStatus != WorkOrderStatus.COMPLETED && saved.isCompleted()) {
            events.publish(new WorkOrderCompleted(saved.getId(), saved.getOrderId(),
                    saved.getCompletedAt()));
        }

        return WorkOrderDetailDto.from(saved, queryService.resolveClientDevices(saved));
    }

    /**
     * Traduce el estado pedido a la operación de dominio correspondiente. La validez
     * de la transición la comprueba el propio agregado.
     */
    private void applyStatusChange(WorkOrder wo, WorkOrderStatus previousStatus,
                                   PatchWorkOrderRequest req) {
        WorkOrderStatus target = WorkOrderStatus.fromApi(req.status());

        switch (target) {
            case COMPLETED -> wo.complete(LocalDateTime.now());
            case CANCELLED -> wo.cancel(req.cancellationReason());
            default -> wo.changeStatus(target);
        }

        // Arrancar un mantenimiento arrastra a los sensores averiados del cliente.
        if (wo.startsMaintenance(previousStatus)) {
            events.publish(new MaintenanceStarted(wo.getId(), wo.clientUserId()));
        }
        if (target == WorkOrderStatus.CANCELLED) {
            events.publish(new WorkOrderCancelled(wo.getId(), wo.getOrderId(),
                    req.cancellationReason()));
        }
    }

    private User requireTechnician(Long technicianId) {
        return userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", technicianId));
    }
}
