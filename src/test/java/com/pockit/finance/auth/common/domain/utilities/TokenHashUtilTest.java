package com.pockit.finance.auth.common.domain.utilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenHashUtilTest {

    @Test
    @DisplayName("generateOpaqueToken debe generar tokens Base64 URL-safe de 43 caracteres sin padding y unicos")
    void shouldGenerateUniqueUrlSafeOpaqueTokensWithoutPadding() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String token = TokenHashUtil.generateOpaqueToken();
            assertThat(token)
                    .hasSize(43)
                    .matches("^[A-Za-z0-9_-]{43}$")
                    .doesNotContain("=");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(50);
    }

    @Test
    @DisplayName("sha256Hex debe generar hash hexadecimal en minusculas de 64 caracteres determinista")
    void shouldComputeDeterministicLowercaseSha256Hex() {
        String rawToken = "test-opaque-token-123";
        String hash1 = TokenHashUtil.sha256Hex(rawToken);
        String hash2 = TokenHashUtil.sha256Hex(rawToken);

        assertThat(hash1)
                .hasSize(64)
                .matches("^[0-9a-f]{64}$")
                .isEqualTo(hash2);
        assertThat(TokenHashUtil.sha256Hex("other-token")).isNotEqualTo(hash1);
    }

    @Test
    @DisplayName("sha256Hex debe lanzar IllegalArgumentException si el token es nulo o vacio")
    void shouldThrowWhenRawTokenIsNullOrBlank() {
        assertThatThrownBy(() -> TokenHashUtil.sha256Hex(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenHashUtil.sha256Hex("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
