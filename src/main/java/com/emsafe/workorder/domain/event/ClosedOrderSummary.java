package com.emsafe.workorder.domain.event;

import com.emsafe.workorder.domain.model.WorkOrder;

/**
 * Retrato de una orden en el instante en que se cierra, que viaja dentro de
 * {@link WorkOrderCompleted} y {@link WorkOrderCancelled}.
 *
 * <p><b>Por qué el evento lleva los datos y no solo el id.</b> El suscriptor de
 * ServiceRecord necesitaba estos seis campos para emitir el acta, y hasta ahora los
 * conseguía volviendo a cargar la orden con el repositorio de WorkOrder — es decir,
 * un contexto importando la <i>infraestructura</i> de otro, justo el acoplamiento que
 * los eventos vienen a romper. Poniéndolos en el evento, ServiceRecord deja de
 * conocer a WorkOrder por completo: solo conoce el hecho publicado.
 *
 * <p>Además es más correcto: el acta debe reflejar lo que era cierto <b>al cerrar</b>,
 * no lo que diga la orden cuando alguien la lea después.
 */
public record ClosedOrderSummary(String client, String site, String serviceType,
                                 String technician, String technicianInitials,
                                 Long technicianId) {

    public static ClosedOrderSummary of(WorkOrder wo) {
        return new ClosedOrderSummary(
                wo.getClient(),
                wo.getLocation(),
                wo.getType() != null ? wo.getType().displayValue() : null,
                wo.getTechnicianName(),
                wo.getTechnicianInitials(),
                wo.technicianId());
    }
}
