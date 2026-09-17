package com.emsafe.alerting.application;

import com.emsafe.alerting.domain.model.Alert;
import com.emsafe.alerting.domain.model.AlertType;
import com.emsafe.alerting.domain.repository.AlertRepository;
import com.emsafe.iam.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * La regla anti-avalancha de las alarmas por nivel crítico.
 *
 * <p>El edge reporta cada pocos segundos. Sin esta guarda, un sensor que se quede en
 * DANGER media hora llenaría el panel de cientos de avisos idénticos.
 *
 * <p>El repositorio se sustituye por un doble en memoria: el caso de uso se prueba sin
 * base de datos porque depende de un <b>puerto</b>, no de Spring Data.
 */
class AlarmApplicationServiceTest {

    /** Doble del puerto: guarda en una lista y responde la pregunta del episodio abierto. */
    private static class RepositorioEnMemoria implements AlertRepository {
        final List<Alert> guardadas = new ArrayList<>();

        @Override public boolean existsUnresolvedByTypeAndSensor(AlertType type, String sensor) {
            return sensor != null && guardadas.stream().anyMatch(a ->
                    a.getType() == type
                            && sensor.equals(a.getSensor())
                            && Boolean.FALSE.equals(a.getResolved()));
        }
        @Override public Alert save(Alert alert) { guardadas.add(alert); return alert; }

        @Override public java.util.Optional<Alert> findById(Long id) { return java.util.Optional.empty(); }
        @Override public List<Alert> findAllNewestFirst() { return guardadas; }
        @Override public long countByType(AlertType type) { return 0; }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Alert> saveAll(List<Alert> alerts) { return alerts; }
        @Override public void deleteById(Long id) { }
        @Override public long count() { return guardadas.size(); }
    }

    private RepositorioEnMemoria repo;
    private AlarmApplicationService servicio;

    @BeforeEach
    void setUp() {
        repo = new RepositorioEnMemoria();
        servicio = new AlarmApplicationService(repo, mock(UserRepository.class));
    }

    @Test
    @DisplayName("Una lectura crítica levanta la alarma")
    void una_lectura_critica_levanta_la_alarma() {
        boolean creada = servicio.raiseDangerDetected("EMSAFE-6766-01", 250.4, 8L, "Quantum Dynamics");

        assertThat(creada).isTrue();
        assertThat(repo.guardadas).hasSize(1);

        Alert a = repo.guardadas.get(0);
        assertThat(a.getType()).isEqualTo(AlertType.DANGER);
        assertThat(a.getSensor()).isEqualTo("EMSAFE-6766-01");
        assertThat(a.getDescription()).contains("250.4").contains("200");
        assertThat(a.isCritical()).isTrue();
    }

    @Test
    @DisplayName("El mismo sensor no vuelve a avisar mientras el episodio siga abierto")
    void no_se_duplica_mientras_el_episodio_sigue_abierto() {
        // El edge reporta 10 veces seguidas con el sensor en DANGER.
        for (int i = 0; i < 10; i++) {
            servicio.raiseDangerDetected("EMSAFE-6766-01", 250.0 + i, 8L, "Quantum");
        }

        assertThat(repo.guardadas)
                .as("10 lecturas críticas del mismo sensor = 1 sola alarma")
                .hasSize(1);
    }

    @Test
    @DisplayName("Cada sensor tiene su propio episodio")
    void sensores_distintos_avisan_por_separado() {
        servicio.raiseDangerDetected("EMSAFE-6766-01", 250.0, 8L, "Quantum");
        servicio.raiseDangerDetected("EMSAFE-6766-02", 260.0, 8L, "Quantum");

        assertThat(repo.guardadas).hasSize(2);
    }

    @Test
    @DisplayName("Tras resolver la alarma, un nuevo pico vuelve a avisar")
    void tras_resolver_se_puede_volver_a_avisar() {
        servicio.raiseDangerDetected("EMSAFE-6766-01", 250.0, 8L, "Quantum");
        repo.guardadas.get(0).resolve(java.time.LocalDateTime.now());

        boolean creada = servicio.raiseDangerDetected("EMSAFE-6766-01", 270.0, 8L, "Quantum");

        assertThat(creada).isTrue();
        assertThat(repo.guardadas).hasSize(2);
    }

    @Test
    @DisplayName("La alarma se dirige al dueño del sensor")
    void la_alarma_va_al_cliente_dueño_del_sensor() {
        servicio.raiseDangerDetected("EMSAFE-6766-01", 250.4, 8L, "Quantum Dynamics");

        Alert a = repo.guardadas.get(0);
        assertThat(a.isForAllClients()).isFalse();
        assertThat(a.isVisibleTo(8L)).isTrue();
        assertThat(a.isVisibleTo(9L))
                .as("un cliente no puede ver la alarma del sensor de otro")
                .isFalse();
    }

    @Test
    @DisplayName("Un sensor todavía en el pool avisa a todos")
    void un_sensor_sin_cliente_avisa_a_todos() {
        // Sensor descubierto por el edge y aún sin reclamar: no tiene dueño.
        servicio.raiseDangerDetected("EMSAFE-NUEVO-99", 250.4, null, null);

        assertThat(repo.guardadas.get(0).isForAllClients()).isTrue();
    }

    @Test
    @DisplayName("Una lectura sin valor no rompe la descripción")
    void una_lectura_sin_valor_no_revienta() {
        boolean creada = servicio.raiseDangerDetected("EMSAFE-6766-01", null, 8L, "Quantum");

        assertThat(creada).isTrue();
        assertThat(repo.guardadas.get(0).getDescription()).isNotBlank();
    }
}
