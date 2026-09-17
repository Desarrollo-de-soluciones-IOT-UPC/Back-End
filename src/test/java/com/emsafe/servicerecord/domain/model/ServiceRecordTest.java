package com.emsafe.servicerecord.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El agregado {@link ServiceRecord}: el acta que queda al cerrar una orden.
 *
 * <p>Es un <b>snapshot inmutable</b>: no tiene un solo método de mutación ni builder
 * público. Si mañana la orden se reasigna, el acta no cambia — eso es lo que la hace
 * un registro histórico y no un JOIN.
 */
class ServiceRecordTest {

    @Nested
    @DisplayName("Emisión al cerrar una orden")
    class Emision {

        @Test
        void desglosa_la_fecha_y_la_hora_de_cierre() {
            // Este formato vivía dentro de WorkOrderService, que tenía que acordarse
            // del patrón "hh:mm a". Ahora es conocimiento de este contexto.
            ServiceRecord acta = ServiceRecord.forClosedOrder(
                    42L, "#WO-0042", "Clínica San Pablo", "Av. Javier Prado 499",
                    "Installation", "Marcus Rivera", "MR", 7L,
                    ServiceRecordStatus.COMPLETED,
                    LocalDateTime.of(2026, 9, 3, 14, 30));

            assertThat(acta.getCompletionDate()).isEqualTo(LocalDate.of(2026, 9, 3));
            assertThat(acta.getCompletionTime()).isEqualTo("02:30 PM");
        }

        @Test
        void la_hora_va_en_ingles_pase_lo_que_pase() {
            // Bug latente cazado en la fase 7: con el locale del servidor en español,
            // "hh:mm a" produce "02:30 p. m." y rompe el formato de la columna.
            // El resultado no puede depender de la máquina donde corra el contenedor.
            java.util.Locale original = java.util.Locale.getDefault();
            try {
                java.util.Locale.setDefault(new java.util.Locale("es", "ES"));
                ServiceRecord acta = ServiceRecord.forClosedOrder(
                        1L, "#WO-0001", "Cliente", "Sede", "Maintenance", "Tec", "TC", 1L,
                        ServiceRecordStatus.COMPLETED,
                        LocalDateTime.of(2026, 9, 3, 14, 30));

                assertThat(acta.getCompletionTime()).isEqualTo("02:30 PM");
            } finally {
                java.util.Locale.setDefault(original);
            }
        }

        @Test
        void copia_los_datos_del_cierre_como_snapshot() {
            ServiceRecord acta = ServiceRecord.forClosedOrder(
                    42L, "#WO-0042", "Clínica San Pablo", "Av. Javier Prado 499",
                    "Installation", "Marcus Rivera", "MR", 7L,
                    ServiceRecordStatus.COMPLETED, LocalDateTime.now());

            assertThat(acta.getWorkOrderId()).isEqualTo(42L);
            assertThat(acta.getOrderId()).isEqualTo("#WO-0042");
            assertThat(acta.getClient()).isEqualTo("Clínica San Pablo");
            assertThat(acta.getTechnician()).isEqualTo("Marcus Rivera");
            assertThat(acta.getTechnicianInitials()).isEqualTo("MR");
            assertThat(acta.wasCompleted()).isTrue();
        }

        @Test
        void sin_fecha_de_cierre_usa_el_momento_actual() {
            ServiceRecord acta = ServiceRecord.forClosedOrder(
                    1L, "#WO-0001", "Cliente", "Sede", "Installation", "Tec", "TC", 1L,
                    ServiceRecordStatus.COMPLETED, null);

            assertThat(acta.getCompletionDate()).isEqualTo(LocalDate.now());
        }

        @Test
        void un_acta_de_cancelacion_no_esta_completada() {
            ServiceRecord acta = ServiceRecord.forClosedOrder(
                    1L, "#WO-0001", "Cliente", "Sede", "Installation", "Tec", "TC", 1L,
                    ServiceRecordStatus.CANCELLED, LocalDateTime.now());

            assertThat(acta.wasCompleted()).isFalse();
            assertThat(acta.getStatus()).isEqualTo(ServiceRecordStatus.CANCELLED);
        }

        @Test
        void sin_desenlace_se_asume_completada() {
            ServiceRecord acta = ServiceRecord.forClosedOrder(
                    1L, "#WO-0001", "Cliente", "Sede", "Installation", "Tec", "TC", 1L,
                    null, LocalDateTime.now());

            assertThat(acta.getStatus()).isEqualTo(ServiceRecordStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Actas históricas (semilla y datos previos al sistema)")
    class Historicas {

        @Test
        void conservan_su_hora_verbatim() {
            // Las semillas traen horas raras como "14:30 PM": no se reformatean.
            ServiceRecord acta = ServiceRecord.historical(
                    "#WO-LM-0021", LocalDate.of(2026, 5, 25), "14:30 PM",
                    "Clínica San Pablo", "Av. Javier Prado 499", "Installation",
                    "Marcus Rivera", "MR", ServiceRecordStatus.COMPLETED, 7L);

            assertThat(acta.getCompletionTime()).isEqualTo("14:30 PM");
            assertThat(acta.getWorkOrderId()).isNull();   // no nace de una orden viva
        }
    }

    @Nested
    @DisplayName("Firma del técnico")
    class Firma {

        @Test
        void un_acta_sabe_quien_la_firmo() {
            // Es lo que filtra el historial del portal técnico.
            ServiceRecord acta = ServiceRecord.forClosedOrder(
                    1L, "#WO-0001", "Cliente", "Sede", "Installation", "Marcus", "MR", 7L,
                    ServiceRecordStatus.COMPLETED, LocalDateTime.now());

            assertThat(acta.wasSignedBy(7L)).isTrue();
            assertThat(acta.wasSignedBy(8L)).isFalse();
            assertThat(acta.wasSignedBy(null)).isFalse();
        }
    }
}
