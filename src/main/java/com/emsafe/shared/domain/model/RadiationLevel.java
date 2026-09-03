package com.emsafe.shared.domain.model;

/**
 * Value Object: nivel de alerta de un campo electromagnético (semáforo SAFE/CAUTION/DANGER).
 *
 * <p>Es parte del <b>Shared Kernel</b>: los contextos Monitoring, Reporting y ClientPortal
 * comparten esta misma clasificación, igual que el bounded context {@code monitoring} del
 * edge (ver {@code monitoring/domain/services.py}, umbrales 100/200 µT — ICNIRP 50 Hz).
 *
 * <p><b>Autoridad:</b> el EDGE clasifica cada lectura. El backend <i>confía</i> en ese nivel
 * y nunca reclasifica; los umbrales de aquí son un <b>fallback</b> exclusivo para lecturas que
 * nunca pasaron por el edge (datos de semilla o legacy con {@code level = NULL}).
 *
 * <p>A diferencia de la versión anterior, este VO <b>no conoce la entidad RadiationReading</b>:
 * el Shared Kernel no puede depender de un contexto concreto.
 */
public enum RadiationLevel {

    SAFE("safe", 0),
    CAUTION("caution", 1),
    DANGER("danger", 2);

    /** µT a partir de los cuales una lectura es "caution" (solo fallback). */
    public static final double CAUTION_UT = 100;
    /** µT a partir de los cuales una lectura es "danger" (solo fallback). */
    public static final double DANGER_UT = 200;

    private final String apiValue;
    private final int severity;

    RadiationLevel(String apiValue, int severity) {
        this.apiValue = apiValue;
        this.severity = severity;
    }

    /**
     * Representación estable para la API REST (minúscula). Front y mobile dependen
     * de estos literales exactos: no cambiar.
     */
    public String apiValue() {
        return apiValue;
    }

    public int severity() {
        return severity;
    }

    public boolean isDangerous() {
        return this == DANGER;
    }

    /**
     * Nivel efectivo de una lectura: se respeta el nivel calculado por el edge y solo
     * se recurre a los umbrales cuando ese nivel falta o es inválido.
     *
     * @param reportedLevel nivel informado por el edge (case-insensitive), puede ser null
     * @param valueUT       magnitud del campo en µT, puede ser null
     */
    public static RadiationLevel of(String reportedLevel, Double valueUT) {
        RadiationLevel parsed = parse(reportedLevel);
        if (parsed != null) {
            return parsed;
        }
        return valueUT != null ? byValue(valueUT) : SAFE;
    }

    /** Clasificación por umbral — solo para datos de semilla/legacy sin nivel del edge. */
    public static RadiationLevel byValue(double valueUT) {
        if (valueUT < CAUTION_UT) return SAFE;
        if (valueUT < DANGER_UT) return CAUTION;
        return DANGER;
    }

    /** Parsea el literal de la API; devuelve SAFE si no se reconoce. */
    public static RadiationLevel fromApi(String value) {
        RadiationLevel parsed = parse(value);
        return parsed != null ? parsed : SAFE;
    }

    /** El más severo de los dos. */
    public RadiationLevel worseOf(RadiationLevel other) {
        if (other == null) return this;
        return other.severity > this.severity ? other : this;
    }

    private static RadiationLevel parse(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase();
        for (RadiationLevel level : values()) {
            if (level.apiValue.equals(normalized)) {
                return level;
            }
        }
        return null;
    }
}
