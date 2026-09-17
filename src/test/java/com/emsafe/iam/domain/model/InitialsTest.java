package com.emsafe.iam.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VO {@link Initials}.
 *
 * <p>Al fusionar los contextos {@code auth} y {@code user} salió a la luz que había
 * <b>dos algoritmos distintos</b> para calcular las iniciales del avatar. Se conservan
 * los dos a propósito, para no cambiar comportamiento observable durante la migración.
 *
 * <p>Estos tests documentan esa discrepancia: <b>si algún día se unifican, aquí se ve
 * exactamente qué cambiaría</b>.
 */
class InitialsTest {

    @Test
    @DisplayName("Los dos algoritmos difieren con nombres de 3+ palabras")
    void la_discrepancia_documentada() {
        String nombre = "Ana María López";

        // Registro público: las dos PRIMERAS palabras.
        assertThat(Initials.fromFirstWords(nombre)).isEqualTo("AM");

        // Alta desde el portal admin: la PRIMERA y la ÚLTIMA.
        assertThat(Initials.fromFirstAndLastWord(nombre)).isEqualTo("AL");
    }

    @Test
    void con_dos_palabras_ambos_coinciden() {
        assertThat(Initials.fromFirstWords("Marcus Rivera")).isEqualTo("MR");
        assertThat(Initials.fromFirstAndLastWord("Marcus Rivera")).isEqualTo("MR");
    }

    @Test
    void una_sola_palabra() {
        assertThat(Initials.fromFirstWords("Cher")).isEqualTo("C");
        assertThat(Initials.fromFirstAndLastWord("Cher")).isEqualTo("CH");
    }

    @Test
    void siempre_en_mayuscula() {
        assertThat(Initials.fromFirstWords("marcus rivera")).isEqualTo("MR");
        assertThat(Initials.fromFirstAndLastWord("marcus rivera")).isEqualTo("MR");
    }

    @Test
    void espacios_de_sobra_no_estorban() {
        assertThat(Initials.fromFirstWords("  Marcus   Rivera  ")).isEqualTo("MR");
        assertThat(Initials.fromFirstAndLastWord("  Marcus   Rivera  ")).isEqualTo("MR");
    }

    @Test
    void un_nombre_vacio_no_revienta_el_avatar() {
        assertThat(Initials.fromFirstWords(null)).isEqualTo("??");
        assertThat(Initials.fromFirstWords("   ")).isEqualTo("??");
        assertThat(Initials.fromFirstAndLastWord(null)).isEqualTo("??");
        assertThat(Initials.fromFirstAndLastWord("   ")).isEqualTo("??");
    }
}
