package com.emsafe.workorder.domain.model;

import com.emsafe.iam.domain.model.User;
import com.emsafe.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El agregado {@link WorkOrder}: las invariantes del ciclo de vida de una orden.
 *
 * <p>Antes esto era imposible de probar sin base de datos: la lógica vivía en
 * {@code WorkOrderService}, que inyectaba cinco repositorios de cuatro contextos.
 * Ahora el agregado se instancia con un builder y se le pregunta directamente.
 */
class WorkOrderTest {

    private static WorkOrder pendiente() {
        return WorkOrder.builder()
                .type(WorkOrderType.INSTALLATION)
                .status(WorkOrderStatus.PENDING)
                .client("Clínica San Pablo")
                .location("Av. Javier Prado 499")
                .build();
    }

    private static WorkOrder enCurso() {
        WorkOrder wo = pendiente();
        wo.start();
        return wo;
    }

    private static User tecnico(Long id, String nombre, String iniciales) {
        return User.builder().id(id).name(nombre).initials(iniciales).build();
    }

    @Nested
    @DisplayName("Ciclo de vida")
    class CicloDeVida {

        @Test
        void nace_pendiente_y_arranca() {
            WorkOrder wo = pendiente();
            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.PENDING);

            wo.start();
            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        }

        @Test
        void completar_sella_la_fecha_de_cierre() {
            WorkOrder wo = enCurso();
            LocalDateTime momento = LocalDateTime.of(2026, 9, 3, 14, 30);

            wo.complete(momento);

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            assertThat(wo.getCompletedAt()).isEqualTo(momento);
        }

        @Test
        void completar_sin_fecha_usa_el_momento_actual() {
            WorkOrder wo = enCurso();
            wo.complete(null);
            assertThat(wo.getCompletedAt()).isNotNull();
        }

        @Test
        void no_se_puede_completar_una_orden_que_nunca_arranco() {
            // La invariante que antes se podía saltar llamando a setStatus() a pelo.
            assertThatThrownBy(() -> pendiente().complete(LocalDateTime.now()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        void no_se_puede_reabrir_una_orden_completada() {
            WorkOrder wo = enCurso();
            wo.complete(LocalDateTime.now());

            assertThatThrownBy(() -> wo.changeStatus(WorkOrderStatus.IN_PROGRESS))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void cancelar_guarda_el_motivo() {
            WorkOrder wo = pendiente();
            wo.cancel("El cliente canceló la visita");

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.CANCELLED);
            assertThat(wo.getCancellationReason()).isEqualTo("El cliente canceló la visita");
        }

        @Test
        void una_orden_en_curso_tambien_puede_cancelarse() {
            WorkOrder wo = enCurso();
            wo.cancel("Avería en el equipo");
            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.CANCELLED);
        }

        @Test
        void un_estado_nulo_es_un_400() {
            assertThatThrownBy(() -> pendiente().changeStatus(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("Asignación de técnico")
    class Asignacion {

        @Test
        void asignar_mantiene_coherentes_los_tres_campos_denormalizados() {
            // Antes eran tres asignaciones sueltas en el service y era fácil
            // dejar el nombre de un técnico con el id de otro.
            WorkOrder wo = pendiente();
            wo.assignTechnician(tecnico(7L, "Marcus Rivera", "MR"));

            assertThat(wo.technicianId()).isEqualTo(7L);
            assertThat(wo.getTechnicianName()).isEqualTo("Marcus Rivera");
            assertThat(wo.getTechnicianInitials()).isEqualTo("MR");
        }

        @Test
        void desasignar_limpia_los_tres_campos_a_la_vez() {
            WorkOrder wo = pendiente();
            wo.assignTechnician(tecnico(7L, "Marcus Rivera", "MR"));

            wo.assignTechnician(null);

            assertThat(wo.technicianId()).isNull();
            assertThat(wo.getTechnicianName()).isNull();
            assertThat(wo.getTechnicianInitials()).isNull();
        }

        @Test
        void una_orden_sabe_si_es_de_un_tecnico() {
            // Es la pregunta que responde el control de acceso del portal técnico.
            WorkOrder wo = pendiente();
            wo.assignTechnician(tecnico(7L, "Marcus Rivera", "MR"));

            assertThat(wo.isAssignedTo(7L)).isTrue();
            assertThat(wo.isAssignedTo(8L)).isFalse();
            assertThat(wo.isAssignedTo(null)).isFalse();
        }

        @Test
        void una_orden_sin_tecnico_no_es_de_nadie() {
            assertThat(pendiente().isAssignedTo(7L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Código legible de la orden")
    class CodigoLegible {

        @Test
        void se_genera_a_partir_del_id_de_la_base_de_datos() {
            WorkOrder wo = pendiente();
            wo.setId(42L);

            wo.assignOrderId();

            assertThat(wo.getOrderId()).isEqualTo("#WO-0042");
        }

        @Test
        void no_se_puede_generar_antes_del_primer_save() {
            // Sin id todavía: el código no se inventa.
            WorkOrder wo = pendiente();
            wo.assignOrderId();
            assertThat(wo.getOrderId()).isNull();
        }

        @Test
        void no_se_reescribe_si_la_orden_ya_tiene_codigo() {
            WorkOrder wo = pendiente();
            wo.setId(42L);
            wo.setOrderId("#WO-LM-0021");   // orden histórica, con otro formato

            wo.assignOrderId();

            assertThat(wo.getOrderId()).isEqualTo("#WO-LM-0021");
        }
    }

    @Nested
    @DisplayName("Arranque de mantenimiento")
    class Mantenimiento {

        private WorkOrder ordenDeMantenimientoConCliente() {
            return WorkOrder.builder()
                    .type(WorkOrderType.MAINTENANCE)
                    .status(WorkOrderStatus.PENDING)
                    .client("Hospital Almenara")
                    .clientUser(User.builder().id(3L).name("Harbor Medical").build())
                    .build();
        }

        @Test
        void arrancar_un_mantenimiento_dispara_el_evento() {
            // Este es el gancho que hace que los sensores averiados del cliente
            // pasen a "in-maintenance" (suscriptor en el contexto Device).
            WorkOrder wo = ordenDeMantenimientoConCliente();
            wo.start();

            assertThat(wo.startsMaintenance(WorkOrderStatus.PENDING)).isTrue();
        }

        @Test
        void una_instalacion_no_dispara_mantenimiento() {
            WorkOrder wo = pendiente();   // INSTALLATION
            wo.start();

            assertThat(wo.startsMaintenance(WorkOrderStatus.PENDING)).isFalse();
        }

        @Test
        void un_mantenimiento_sin_cliente_enlazado_no_dispara_nada() {
            WorkOrder wo = WorkOrder.builder()
                    .type(WorkOrderType.MAINTENANCE)
                    .status(WorkOrderStatus.PENDING)
                    .client("Cliente legacy sin FK")
                    .build();
            wo.start();

            assertThat(wo.startsMaintenance(WorkOrderStatus.PENDING)).isFalse();
        }

        @Test
        void no_se_dispara_dos_veces_si_ya_estaba_en_curso() {
            WorkOrder wo = ordenDeMantenimientoConCliente();
            wo.start();

            // El técnico reenvía "in-progress" al guardar notas: no debe re-disparar.
            assertThat(wo.startsMaintenance(WorkOrderStatus.IN_PROGRESS)).isFalse();
        }
    }
}
