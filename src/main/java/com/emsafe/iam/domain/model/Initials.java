package com.emsafe.iam.domain.model;

/**
 * Value Object: iniciales mostradas en los avatares del portal.
 *
 * <p>Al unificar los contextos {@code auth} y {@code user} salió a la luz que había
 * <b>dos algoritmos distintos</b> para lo mismo:
 * <ul>
 *   <li>El registro público tomaba la inicial de las <b>dos primeras palabras</b>.</li>
 *   <li>El alta desde el portal admin tomaba la inicial de la <b>primera y la última</b>.</li>
 * </ul>
 *
 * <p>Ambos se conservan aquí como factorías separadas para <b>no cambiar el comportamiento
 * observable</b> durante la migración (regla R1/R5 del plan). Unificarlos es una decisión
 * de negocio pendiente, no un refactor.
 */
public final class Initials {

    private Initials() {
    }

    /** Iniciales de las dos primeras palabras — comportamiento del registro público. */
    public static String fromFirstWords(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.toString();
    }

    /** Iniciales de la primera y la última palabra — comportamiento del alta admin. */
    public static String fromFirstAndLastWord(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
