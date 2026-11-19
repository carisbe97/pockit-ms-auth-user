package com.pockit.finance.auth.common.domain.utilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailUtilTest {

    @Test
    @DisplayName("Debe normalizar correos eliminando espacios y convirtiendo a minusculas")
    void shouldNormalizeEmail() {
        assertThat(EmailUtil.normalize(null)).isNull();
        assertThat(EmailUtil.normalize("  Camilo.Perez+Tag@Pockit.COM  "))
                .isEqualTo("camilo.perez+tag@pockit.com");
    }

    @Test
    @DisplayName("Debe validar correos validos e invalidos correctamente")
    void shouldValidateEmails() {
        assertThat(EmailUtil.isValid("usuario@pockit.com")).isTrue();
        assertThat(EmailUtil.isValid("  USER+123@sub.dominio.co  ")).isTrue();
        assertThat(EmailUtil.isValid("a.b_c-d@example.org")).isTrue();

        assertThat(EmailUtil.isValid(null)).isFalse();
        assertThat(EmailUtil.isValid("")).isFalse();
        assertThat(EmailUtil.isValid("   ")).isFalse();
        assertThat(EmailUtil.isValid("sin-arroba.com")).isFalse();
        assertThat(EmailUtil.isValid("usuario@")).isFalse();
        assertThat(EmailUtil.isValid("@dominio.com")).isFalse();
        assertThat(EmailUtil.isValid("usuario@dominio")).isFalse();
        assertThat(EmailUtil.isValid("usuario..doble@dominio.com")).isFalse();
        assertThat(EmailUtil.isValid("a".repeat(250) + "@b.com")).isFalse();
    }
}
