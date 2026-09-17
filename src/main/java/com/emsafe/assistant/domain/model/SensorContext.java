package com.emsafe.assistant.domain.model;

/**
 * Lo que Assistant necesita saber de un sensor para poder responder — y nada más.
 *
 * <p>Es la <b>capa anticorrupción de entrada</b>: antes, el service del asistente llamaba
 * a {@code ClientService.getDevices()} y navegaba el DTO del portal móvil, de modo que
 * este contexto quedaba atado a la forma de la pantalla de otro. Ahora el llamador
 * traduce a este vocabulario propio y Assistant no conoce ni clientes ni dispositivos.
 *
 * @param name     nombre visible del sensor
 * @param zone     zona o sala; {@code null} si no está registrada
 * @param valueUT  última medida en µT; {@code null} si aún no ha reportado
 * @param level    nivel calculado por el edge ("safe" | "caution" | "danger")
 * @param plug     estado del relé ("ON" | "OFF"); {@code null} si no aplica
 */
public record SensorContext(String name, String zone, Double valueUT, String level, String plug) {

    public boolean hasReadings() {
        return valueUT != null;
    }
}
