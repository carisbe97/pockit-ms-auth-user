package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.LoginCommand;
import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class LoginServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-22T18:00:00Z");
    private static final long REFRESH_EXPIRATION_SECONDS = 604800L;

    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private IPasswordHasherPort passwordHasherPort;
    @Mock
    private ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;
    @Mock
    private ITokenPort tokenPort;

    private Clock fixedClock;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        loginService = new LoginService(
                usuarioRepositoryPort,
                passwordHasherPort,
                sesionRefreshRepositoryPort,
                tokenPort,
                REFRESH_EXPIRATION_SECONDS,
                fixedClock
        );
    }

    @Test
    @DisplayName("Debe autenticar usuario activo, emitir JWT y persistir hash SHA-256 del refresh token")
    void shouldLoginActiveUserAndPersistRefreshSession() {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$hash")
                .usrNombre("Camilo")
                .usrApellido("Perez")
                .usrActivo(Boolean.TRUE)
                .build();

        when(usuarioRepositoryPort.findByCorreo("camilo@pockit.com")).thenReturn(Optional.of(usuario));
        when(passwordHasherPort.matches("Pockit@2026", "$2a$12$hash")).thenReturn(true);
        when(tokenPort.generarAccessToken(usuario)).thenReturn("jwt-access-token");
        when(tokenPort.obtenerExpiracionSegundos()).thenReturn(900L);
        when(sesionRefreshRepositoryPort.save(any(SesionRefresh.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = loginService.ejecutar(new LoginCommand("  CAMILO@Pockit.COM ", "Pockit@2026", "Web Chrome"));

        assertThat(result.accessToken()).isEqualTo("jwt-access-token");
        assertThat(result.refreshToken()).isNotBlank().hasSize(43);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(900L);
        assertThat(result.usuario()).isSameAs(usuario);

        ArgumentCaptor<SesionRefresh> sesionCaptor = ArgumentCaptor.forClass(SesionRefresh.class);
        verify(sesionRefreshRepositoryPort).save(sesionCaptor.capture());
        SesionRefresh saved = sesionCaptor.getValue();

        assertThat(saved.getSrfUsrId()).isEqualTo(usrId);
        assertThat(saved.getSrfTokenHash()).isEqualTo(TokenHashUtil.sha256Hex(result.refreshToken()));
        assertThat(saved.getSrfTokenHash()).isNotEqualTo(result.refreshToken());
        assertThat(saved.getSrfRevocado()).isFalse();
        assertThat(saved.getSrfDispositivo()).isEqualTo("Web Chrome");
        OffsetDateTime expectedNow = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        assertThat(saved.getSrfCreadoEn()).isEqualTo(expectedNow);
        assertThat(saved.getSrfExpiraEn()).isEqualTo(expectedNow.plusSeconds(REFRESH_EXPIRATION_SECONDS));
    }

    @Test
    @DisplayName("Debe lanzar 401 CREDENCIALES_INVALIDAS si el usuario no existe")
    void shouldFailWith401WhenUserNotFound() {
        when(usuarioRepositoryPort.findByCorreo("noexiste@pockit.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.ejecutar(new LoginCommand("noexiste@pockit.com", "Pockit@2026", null)))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo("CREDENCIALES_INVALIDAS");
                });

        verify(sesionRefreshRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 401 CREDENCIALES_INVALIDAS si la contrasena no coincide")
    void shouldFailWith401WhenPasswordDoesNotMatch() {
        Usuario usuario = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$hash")
                .usrActivo(Boolean.TRUE)
                .build();

        when(usuarioRepositoryPort.findByCorreo("camilo@pockit.com")).thenReturn(Optional.of(usuario));
        when(passwordHasherPort.matches("wrong-pass", "$2a$12$hash")).thenReturn(false);

        assertThatThrownBy(() -> loginService.ejecutar(new LoginCommand("camilo@pockit.com", "wrong-pass", null)))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo("CREDENCIALES_INVALIDAS");
                });

        verify(sesionRefreshRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 401 CREDENCIALES_INVALIDAS si el usuario esta inactivo")
    void shouldFailWith401WhenUserIsInactive() {
        Usuario usuario = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrCorreo("camilo@pockit.com")
                .usrPasswordHash("$2a$12$hash")
                .usrActivo(Boolean.FALSE)
                .build();

        when(usuarioRepositoryPort.findByCorreo("camilo@pockit.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> loginService.ejecutar(new LoginCommand("camilo@pockit.com", "Pockit@2026", null)))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo("CREDENCIALES_INVALIDAS");
                });

        verify(sesionRefreshRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 400 VALIDATION_ERROR cuando el comando de login es nulo")
    void shouldFailWith400WhenCommandIsNull() {
        assertThatThrownBy(() -> loginService.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(400);
                    assertThat(domainEx.getCode()).isEqualTo("VALIDATION_ERROR");
                });
    }
}
