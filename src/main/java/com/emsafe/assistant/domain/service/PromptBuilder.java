package com.emsafe.assistant.domain.service;

import com.emsafe.assistant.domain.model.SensorContext;
import com.emsafe.shared.domain.model.RadiationLevel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Domain service: redacta las instrucciones con las que se fundamenta a Astra.
 *
 * <p>Es un domain service y no un método suelto porque el prompt <b>es</b> la regla de
 * negocio del asistente: qué umbrales cita, en qué unidad, qué NO debe inventar y qué
 * deriva a un profesional. Los umbrales los toma del VO compartido
 * {@link RadiationLevel}, así que cambiar la escala en un sitio cambia lo que Astra dice
 * — antes eran números escritos dentro de un {@code StringBuilder} de 15 líneas.
 */
@Component
public class PromptBuilder {

    public String build(List<SensorContext> sensors) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are Astra, the assistant inside the EMSafe mobile app. ")
          .append("EMSafe monitors electromagnetic field exposure (magnetic field, microtesla µT) ")
          .append("with IoT sensors. Levels (ICNIRP 50 Hz reference): safe < ")
          .append((int) RadiationLevel.CAUTION_UT).append(" µT, caution ")
          .append((int) RadiationLevel.CAUTION_UT).append("–")
          .append((int) RadiationLevel.DANGER_UT).append(" µT, danger ≥ ")
          .append((int) RadiationLevel.DANGER_UT).append(" µT. ")
          .append("Each sensor may control a smart plug (relay) that can cut power; ")
          .append("the user can toggle it from the Monitor tab. ")
          .append("Answer briefly (max ~120 words), in the same language the user writes ")
          .append("(Spanish or English). Be practical and reassuring; do not invent data. ")
          .append("If asked about medical issues, recommend consulting a professional.\n\n");

        sb.append("Current sensors of this user:\n");
        if (sensors == null || sensors.isEmpty()) {
            sb.append("- (no sensors assigned yet)\n");
            return sb.toString();
        }

        for (SensorContext s : sensors) {
            sb.append("- ").append(s.name())
              .append(" [").append(s.zone() == null ? "no zone" : s.zone()).append("]: ");
            if (s.hasReadings()) {
                sb.append(s.valueUT()).append(" µT (").append(s.level()).append(")");
            } else {
                sb.append("no readings yet");
            }
            if (s.plug() != null) {
                sb.append(", plug ").append(s.plug());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
