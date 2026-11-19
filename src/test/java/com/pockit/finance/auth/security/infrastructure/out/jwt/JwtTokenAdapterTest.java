package com.pockit.finance.auth.security.infrastructure.out.jwt;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.users.domain.models.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenAdapterTest {

    private static final String SECRET_64_BYTES = "pockit-test-jwt-secret-key-minimo-64-bytes-para-hs512-0123456789abcdef";
    private static final String ISSUER = "pockit-ms-auth-user";
    private static final Instant FIXED_NOW = Instant.parse("2026-11-19T12:00:00Z");

    private JwtTokenAdapter tokenAdapter;

    @BeforeEach
    void setUp() {
        tokenAdapter = new JwtTokenAdapter(SECRET_64_BYTES, ISSUER, 900, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Debe exigir secreto de al menos 64 bytes para HS512 (fail-fast)")
    void shouldFailFastIfSecretTooShort() {
        assertThatThrownBy(() -> new JwtTokenAdapter("short-secret", ISSUER, 900, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtTokenAdapter(null, ISSUER, 900, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Debe generar un JWT valido y extraer el UUID del usuario")
    void shouldGenerateAndValidateJwt() {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .build();

        String jwt = tokenAdapter.generarAccessToken(usuario);
        UUID extractedId = tokenAdapter.validarYExtraerUsuarioId(jwt);

        assertThat(extractedId).isEqualTo(usrId);
        assertThat(tokenAdapter.obtenerExpiracionSegundos()).isEqualTo(900L);
    }

    @Test
    @DisplayName("Debe lanzar TOKEN_EXPIRADO (401) cuando el token ha expirado")
    void shouldRejectExpiredToken() {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().usrId(usrId).usrCorreo("camilo@pockit.com").build();
        String jwt = tokenAdapter.generarAccessToken(usuario);

        JwtTokenAdapter futureAdapter = new JwtTokenAdapter(
                SECRET_64_BYTES,
                ISSUER,
                900,
                Clock.fixed(FIXED_NOW.plusSeconds(901), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> futureAdapter.validarYExtraerUsuarioId(jwt))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo(BaseConstants.CODE_TOKEN_EXPIRADO);
                });
    }

    @Test
    @DisplayName("Debe lanzar TOKEN_INVALIDO (401) ante firma adulterada, issuer distinto, vacio o usuario invalido")
    void shouldRejectInvalidTokens() {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().usrId(usrId).usrCorreo("camilo@pockit.com").build();
        String jwt = tokenAdapter.generarAccessToken(usuario);

        JwtTokenAdapter otherIssuerAdapter = new JwtTokenAdapter(
                SECRET_64_BYTES,
                "otro-emisor",
                900,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> otherIssuerAdapter.validarYExtraerUsuarioId(jwt))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode())
                        .isEqualTo(BaseConstants.CODE_TOKEN_INVALIDO));

        assertThatThrownBy(() -> tokenAdapter.validarYExtraerUsuarioId(jwt + "tampered"))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode())
                        .isEqualTo(BaseConstants.CODE_TOKEN_INVALIDO));

        assertThatThrownBy(() -> tokenAdapter.validarYExtraerUsuarioId(""))
                .isInstanceOf(BaseDomainException.class);
        assertThatThrownBy(() -> tokenAdapter.validarYExtraerUsuarioId(null))
                .isInstanceOf(BaseDomainException.class);
        assertThatThrownBy(() -> tokenAdapter.generarAccessToken(null))
                .isInstanceOf(BaseDomainException.class);
    }
}
