package com.emsafe.servicerecord.application;

import com.emsafe.servicerecord.domain.model.ServiceRecord;
import com.emsafe.servicerecord.domain.model.ServiceRecordStatus;
import com.emsafe.servicerecord.domain.repository.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lado de ESCRITURA del contexto ServiceRecord: emitir el acta de una orden cerrada.
 *
 * <p>Es el único caso de uso que hay, y no lo invoca ningún controller: lo dispara
 * {@code WorkOrderLifecycleHandler} al recibir el evento de cierre. Recibe datos
 * planos a propósito — así este servicio no conoce ningún tipo del contexto
 * WorkOrder; la traducción del evento la hace el suscriptor.
 */
@Service
@RequiredArgsConstructor
public class ServiceRecordApplicationService {

    private final ServiceRecordRepository repository;

    @Transactional
    public void recordClosure(Long workOrderId, String orderId,
                              String client, String site, String serviceType,
                              String technician, String technicianInitials, Long technicianId,
                              ServiceRecordStatus status, LocalDateTime closedAt) {
        repository.save(ServiceRecord.forClosedOrder(
                workOrderId, orderId, client, site, serviceType,
                technician, technicianInitials, technicianId, status, closedAt));
    }
}
