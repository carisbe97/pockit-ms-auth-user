package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.users.domain.models.RestablecerPasswordCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestablecerPasswordServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-22T18:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private IPasswordHasherPort passwordHasherPort;

    @Mock
    private ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;

    private RestablecerPasswordService service;

    @BeforeEach
    void setUp() {
        service = new RestablecerPasswordService(
                usuarioRepositoryPort, passwordHasherPort, sesionRefreshRepositoryPort, FIXED_CLOCK
        );
    }

    @Test
    @DisplayName("Debe restablecer contrasena, limpiar token de recuperacion y revocar sesiones activas")
    void shouldResetPasswordClearTokenAndRevokeSessions() {
        UUID usrId = UUID.randomUUID();
        String rawToken = "valid-opaque-reset-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        OffsetDateTime ahora = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$oldHash")
                .usrActivo(true)
                .usrResetTokenHash(tokenHash)
                .usrResetExpiraEn(ahora.plusMinutes(10))
                .build();

        when(usuarioRepositoryPort.findByResetTokenHash(tokenHash)).thenReturn(Optional.of(usuario));
        when(passwordHasherPort.hash("NuevaClave@2026")).thenReturn("$2a$12$newHash");
        when(usuarioRepositoryPort.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar(new RestablecerPasswordCommand(rawToken, "NuevaClave@2026"));

        assertThat(usuario.getUsrPasswordHash()).isEqualTo("$2a$12$newHash");
        assertThat(usuario.getUsrResetTokenHash()).isNull();
        assertThat(usuario.getUsrResetExpiraEn()).isNull();
        verify(usuarioRepositoryPort).save(usuario);
        verify(sesionRefreshRepositoryPort).revocarTodasPorUsuario(usrId);
    }

    @Test
    @DisplayName("Debe lanzar 400 RESET_TOKEN_INVALIDO cuando el token no existe o esta expirado o usuario inactivo")
    void shouldThrow400WhenTokenInvalidOrExpired() {
        String rawToken = "expired-or-unknown-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        OffsetDateTime ahora = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

        when(usuarioRepositoryPort.findByResetTokenHash(tokenHash)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ejecutar(new RestablecerPasswordCommand(rawToken, "NuevaClave@2026")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(400);
                    assertThat(bde.getCode()).isEqualTo("RESET_TOKEN_INVALIDO");
                });

        Usuario expirado = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrActivo(true)
                .usrResetTokenHash(tokenHash)
                .usrResetExpiraEn(ahora.minusMinutes(1))
                .build();
        when(usuarioRepositoryPort.findByResetTokenHash(tokenHash)).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.ejecutar(new RestablecerPasswordCommand(rawToken, "NuevaClave@2026")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo("RESET_TOKEN_INVALIDO"));

        Usuario inactivo = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrActivo(false)
                .usrResetTokenHash(tokenHash)
                .usrResetExpiraEn(ahora.plusMinutes(5))
                .build();
        when(usuarioRepositoryPort.findByResetTokenHash(tokenHash)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> service.ejecutar(new RestablecerPasswordCommand(rawToken, "NuevaClave@2026")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo("RESET_TOKEN_INVALIDO"));

        verify(usuarioRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 422 PASSWORD_DEBIL cuando la nueva contrasena no cumple politica de fortaleza")
    void shouldThrow422WhenPasswordIsWeak() {
        assertThatThrownBy(() -> service.ejecutar(new RestablecerPasswordCommand("some-token", "debil123")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(422);
                    assertThat(bde.getCode()).isEqualTo("PASSWORD_DEBIL");
                });
    }

    @Test
    @DisplayName("Debe lanzar 400 cuando el comando o token es nulo o blanco")
    void shouldThrow400WhenCommandOrTokenBlank() {
        assertThatThrownBy(() -> service.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getStatusCode()).isEqualTo(400));

        assertThatThrownBy(() -> service.ejecutar(new RestablecerPasswordCommand(" ", "NuevaClave@2026")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo("RESET_TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("Constructor sin Clock debe usar Clock.systemUTC por defecto")
    void shouldSupportDefaultUtcClockConstructor() {
        RestablecerPasswordService defaultService = new RestablecerPasswordService(
                usuarioRepositoryPort, passwordHasherPort, sesionRefreshRepositoryPort
        );
        when(usuarioRepositoryPort.findByResetTokenHash(TokenHashUtil.sha256Hex("missing")))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> defaultService.ejecutar(new RestablecerPasswordCommand("missing", "NuevaClave@2026")))
                .isInstanceOf(BaseDomainException.class);
    }
}
