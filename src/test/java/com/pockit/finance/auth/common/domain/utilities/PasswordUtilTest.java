package com.pockit.finance.auth.common.domain.utilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    @DisplayName("Debe aceptar contrasenas fuertes y rechazar contrasenas debiles")
    void shouldValidatePasswordStrength() {
        assertThat(PasswordUtil.isStrong("Pockit@2026")).isTrue();
        assertThat(PasswordUtil.isStrong("Aa1!bcde")).isTrue();

        assertThat(PasswordUtil.isStrong(null)).isFalse();
        assertThat(PasswordUtil.isStrong("Short1!")).isFalse();
        assertThat(PasswordUtil.isStrong("A".repeat(129) + "a1!")).isFalse();
        assertThat(PasswordUtil.isStrong("solominusculas1!")).isFalse();
        assertThat(PasswordUtil.isStrong("SOLOMAYUSCULAS1!")).isFalse();
        assertThat(PasswordUtil.isStrong("SinNumeros!")).isFalse();
        assertThat(PasswordUtil.isStrong("SinEspecial123")).isFalse();
        assertThat(PasswordUtil.isStrong("Con Espacio1!")).isFalse();
    }
}
