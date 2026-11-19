package com.pockit.finance.auth.users.infrastructure.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherAdapterTest {

    @Test
    @DisplayName("Debe hashear y verificar contrasenas con BCrypt")
    void shouldHashAndMatchPassword() {
        BCryptPasswordHasherAdapter adapter = new BCryptPasswordHasherAdapter(new BCryptPasswordEncoder(4));

        String hash = adapter.hash("Pockit@2026");

        assertThat(hash).isNotEqualTo("Pockit@2026");
        assertThat(adapter.matches("Pockit@2026", hash)).isTrue();
        assertThat(adapter.matches("OtraClave@2026", hash)).isFalse();
    }
}
