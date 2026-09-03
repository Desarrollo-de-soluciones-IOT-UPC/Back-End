package com.emsafe.alerting.application;

import com.emsafe.alerting.domain.model.Alert;
import com.emsafe.alerting.domain.model.AlertType;
import com.emsafe.alerting.domain.repository.AlertRepository;
import com.emsafe.alerting.interfaces.rest.dto.AlertDto;
import com.emsafe.alerting.interfaces.rest.dto.CreateAlarmRequest;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Casos de uso del contexto Alerting.
 *
 * <p>La coherencia "destinatario específico ⇒ hay ids ⇒ hay nombres visibles" ya no
 * se mantiene aquí a mano: la garantizan las factorías del agregado.
 */
@Service
@RequiredArgsConstructor
public class AlarmApplicationService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public List<AlertDto> getAll() {
        return alertRepository.findAllNewestFirst().stream().map(AlertDto::from).toList();
    }

    public long countCritical() {
        return alertRepository.countByType(AlertType.DANGER);
    }

    @Transactional
    public AlertDto create(CreateAlarmRequest req) {
        boolean specific = "specific".equalsIgnoreCase(req.recipientType());
        AlertType type = AlertType.fromPersisted(req.type());

        Alert alert;
        if (specific && req.clientIds() != null && !req.clientIds().isEmpty()) {
            List<User> clients = userRepository.findAllById(req.clientIds());
            alert = Alert.forClients(
                    type, req.icon(), req.title(), req.description(),
                    req.relativeTime(), req.sensor(),
                    clients.stream().map(User::getId).toList(),
                    clients.stream().map(User::getName).collect(Collectors.joining(", ")));
        } else {
            alert = Alert.forAllClients(
                    type, req.icon(), req.title(), req.description(),
                    req.relativeTime(), req.sensor());
        }

        return AlertDto.from(alertRepository.save(alert));
    }

    /**
     * Alarma informativa por la creación de una orden de trabajo.
     *
     * <p>Este método existe para que {@code WorkOrderService} deje de construir
     * entidades {@code Alert} a mano. En la fase 6 pasará a ser un suscriptor del
     * evento de dominio {@code WorkOrderCreated} y el contexto WorkOrder dejará de
     * conocer a Alerting por completo.
     */
    @Transactional
    public void raiseOrderCreated(String orderId, String clientDisplayName,
                                  String technicianName, Long clientUserId) {
        String techPart = technicianName != null && !technicianName.isBlank()
                ? " assigned to " + technicianName : "";
        String description = String.format("%s — %s%s.", orderId, clientDisplayName, techPart);

        Alert alert = clientUserId != null
                ? Alert.forClients(AlertType.INFO, "ph-clipboard-text", "New work order created",
                        description, "Just now", null, List.of(clientUserId), clientDisplayName)
                : Alert.forAllClients(AlertType.INFO, "ph-clipboard-text", "New work order created",
                        description, "Just now", null);

        alertRepository.save(alert);
    }

    @Transactional
    public AlertDto resolve(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm", id));
        alert.resolve(LocalDateTime.now());
        return AlertDto.from(alertRepository.save(alert));
    }

    @Transactional
    public void delete(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alarm", id);
        }
        alertRepository.deleteById(id);
    }
}
