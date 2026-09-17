package com.emsafe.alerting.application;

import com.emsafe.alerting.domain.model.Alert;
import com.emsafe.alerting.domain.model.AlertType;
import com.emsafe.alerting.domain.repository.AlertRepository;
import com.emsafe.alerting.interfaces.rest.dto.AlertDto;
import com.emsafe.alerting.interfaces.rest.dto.CreateAlarmRequest;
import com.emsafe.iam.domain.model.User;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import com.emsafe.shared.domain.model.RadiationLevel;
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

    /**
     * Alarma por una lectura que superó el umbral crítico.
     *
     * <p>Reacciona al evento {@code DangerLevelDetected} de Monitoring. Los puertos
     * quedaron listos en la fase 5 pero no se conectaron: crear alarmas nuevas es
     * funcionalidad, no refactor (regla R5 del plan DDD).
     *
     * <p><b>Una alarma por episodio, no por lectura.</b> El edge reporta cada pocos
     * segundos; sin esta guarda, un sensor que se quede en DANGER media hora llenaría
     * el panel de cientos de avisos idénticos. Mientras la alarma anterior siga sin
     * resolver, el episodio se considera abierto. Al resolverla, un nuevo pico vuelve
     * a avisar — que es justo lo que se espera de un panel de alarmas.
     *
     * @return {@code true} si se creó la alarma; {@code false} si el episodio ya estaba abierto
     */
    @Transactional
    public boolean raiseDangerDetected(String serialNumber, Double fieldUT,
                                       Long clientId, String clientName) {
        if (alertRepository.existsUnresolvedByTypeAndSensor(AlertType.DANGER, serialNumber)) {
            return false;
        }

        String sensorLabel = serialNumber != null ? serialNumber : "Unknown sensor";
        String description = String.format(
                "%s recorded %s µT (limit %s µT).",
                sensorLabel,
                fieldUT != null ? fieldUT : "—",
                (int) RadiationLevel.DANGER_UT);

        // La alarma se dirige al dueño del sensor; si aún no tiene cliente asignado
        // (sensor en el pool), la ven todos los clientes.
        Alert alert = clientId != null
                ? Alert.forClients(AlertType.DANGER, "ph-warning-octagon",
                        "Critical radiation level", description, "Just now", serialNumber,
                        List.of(clientId), clientName)
                : Alert.forAllClients(AlertType.DANGER, "ph-warning-octagon",
                        "Critical radiation level", description, "Just now", serialNumber);

        alertRepository.save(alert);
        return true;
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
