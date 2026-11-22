package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.users.domain.exceptions.RefreshTokenReuseException;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;
import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.models.Usuario;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-22T18:00:00Z");
    private static final long REFRESH_EXPIRATION_SECONDS = 604800L;

    @Mock
    private ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;
    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private ITokenPort tokenPort;

    private Clock fixedClock;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        refreshTokenService = new RefreshTokenService(
                sesionRefreshRepositoryPort,
                usuarioRepositoryPort,
                tokenPort,
                REFRESH_EXPIRATION_SECONDS,
                fixedClock
        );
    }

    @Test
    @DisplayName("Debe rotar refresh token de un solo uso, revocar el anterior y emitir nuevo par de tokens")
    void shouldRotateValidRefreshTokenAndRevokePreviousSession() {
        UUID usrId = UUID.randomUUID();
        String oldRawToken = "old-valid-refresh-token";
        String oldHash = TokenHashUtil.sha256Hex(oldRawToken);
        OffsetDateTime ahora = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

        SesionRefresh currentSession = SesionRefresh.builder()
                .srfId(10L)
                .srfUsrId(usrId)
                .srfTokenHash(oldHash)
                .srfExpiraEn(ahora.plusDays(3))
                .srfRevocado(Boolean.FALSE)
                .srfDispositivo("iOS App")
                .srfCreadoEn(ahora.minusDays(1))
                .build();

        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrActivo(Boolean.TRUE)
                .build();

        when(sesionRefreshRepositoryPort.findByTokenHash(oldHash)).thenReturn(Optional.of(currentSession));
        when(usuarioRepositoryPort.findById(usrId)).thenReturn(Optional.of(usuario));
        when(tokenPort.generarAccessToken(usuario)).thenReturn("new-jwt-access-token");
        when(tokenPort.obtenerExpiracionSegundos()).thenReturn(900L);
        when(sesionRefreshRepositoryPort.save(any(SesionRefresh.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = refreshTokenService.ejecutar(new RefreshTokenCommand(oldRawToken, null));

        assertThat(result.accessToken()).isEqualTo("new-jwt-access-token");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(oldRawToken);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(900L);
        assertThat(result.usuario()).isSameAs(usuario);

        ArgumentCaptor<SesionRefresh> savedCaptor = ArgumentCaptor.forClass(SesionRefresh.class);
        verify(sesionRefreshRepositoryPort, times(2)).save(savedCaptor.capture());
        List<SesionRefresh> savedSessions = savedCaptor.getAllValues();

        SesionRefresh revokedOld = savedSessions.get(0);
        assertThat(revokedOld.getSrfId()).isEqualTo(10L);
        assertThat(revokedOld.getSrfRevocado()).isTrue();

        SesionRefresh newSession = savedSessions.get(1);
        assertThat(newSession.getSrfUsrId()).isEqualTo(usrId);
        assertThat(newSession.getSrfTokenHash()).isEqualTo(TokenHashUtil.sha256Hex(result.refreshToken()));
        assertThat(newSession.getSrfRevocado()).isFalse();
        assertThat(newSession.getSrfDispositivo()).isEqualTo("iOS App");
        assertThat(newSession.getSrfExpiraEn()).isEqualTo(ahora.plusSeconds(REFRESH_EXPIRATION_SECONDS));
        verify(sesionRefreshRepositoryPort, never()).revocarTodasPorUsuario(any());
    }

    @Test
    @DisplayName("Debe detectar reuso de refresh token revocado, revocar todas las sesiones del usuario y lanzar RefreshTokenReuseException")
    void shouldRevokeAllUserSessionsAndThrowWhenRefreshTokenIsReused() {
        UUID usrId = UUID.randomUUID();
        String reusedRawToken = "reused-revoked-token";
        String reusedHash = TokenHashUtil.sha256Hex(reusedRawToken);
        OffsetDateTime ahora = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

        SesionRefresh revokedSession = SesionRefresh.builder()
                .srfId(20L)
                .srfUsrId(usrId)
                .srfTokenHash(reusedHash)
                .srfExpiraEn(ahora.plusDays(5))
                .srfRevocado(Boolean.TRUE)
                .build();

        when(sesionRefreshRepositoryPort.findByTokenHash(reusedHash)).thenReturn(Optional.of(revokedSession));

        assertThatThrownBy(() -> refreshTokenService.ejecutar(new RefreshTokenCommand(reusedRawToken, "Chrome")))
                .isInstanceOf(RefreshTokenReuseException.class)
                .satisfies(ex -> {
                    RefreshTokenReuseException reuseEx = (RefreshTokenReuseException) ex;
                    assertThat(reuseEx.getStatusCode()).isEqualTo(401);
                    assertThat(reuseEx.getCode()).isEqualTo("REFRESH_TOKEN_INVALIDO");
                });

        verify(sesionRefreshRepositoryPort).revocarTodasPorUsuario(usrId);
        verify(sesionRefreshRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 401 REFRESH_TOKEN_INVALIDO cuando el token ha expirado")
    void shouldThrow401WhenRefreshTokenIsExpired() {
        UUID usrId = UUID.randomUUID();
        String expiredRawToken = "expired-refresh-token";
        String expiredHash = TokenHashUtil.sha256Hex(expiredRawToken);
        OffsetDateTime ahora = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

        SesionRefresh expiredSession = SesionRefresh.builder()
                .srfId(30L)
                .srfUsrId(usrId)
                .srfTokenHash(expiredHash)
                .srfExpiraEn(ahora.minusSeconds(1))
                .srfRevocado(Boolean.FALSE)
                .build();

        when(sesionRefreshRepositoryPort.findByTokenHash(expiredHash)).thenReturn(Optional.of(expiredSession));

        assertThatThrownBy(() -> refreshTokenService.ejecutar(new RefreshTokenCommand(expiredRawToken, null)))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo("REFRESH_TOKEN_INVALIDO");
                });

        verify(sesionRefreshRepositoryPort, never()).revocarTodasPorUsuario(any());
        verify(sesionRefreshRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 401 REFRESH_TOKEN_INVALIDO cuando el token no existe")
    void shouldThrow401WhenRefreshTokenDoesNotExist() {
        String unknownRawToken = "unknown-refresh-token";
        String unknownHash = TokenHashUtil.sha256Hex(unknownRawToken);

        when(sesionRefreshRepositoryPort.findByTokenHash(unknownHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.ejecutar(new RefreshTokenCommand(unknownRawToken, null)))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo("REFRESH_TOKEN_INVALIDO");
                });
    }
}
