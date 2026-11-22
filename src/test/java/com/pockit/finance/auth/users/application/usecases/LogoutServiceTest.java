package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.users.domain.models.LogoutCommand;
import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;

    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        logoutService = new LogoutService(sesionRefreshRepositoryPort);
    }

    @Test
    @DisplayName("Debe revocar la sesion refresh activa correspondiente al token")
    void shouldRevokeActiveRefreshSession() {
        String rawToken = "active-refresh-token";
        String tokenHash = TokenHashUtil.sha256Hex(rawToken);
        SesionRefresh sesion = SesionRefresh.builder()
                .srfId(1L)
                .srfUsrId(UUID.randomUUID())
                .srfTokenHash(tokenHash)
                .srfRevocado(Boolean.FALSE)
                .build();

        when(sesionRefreshRepositoryPort.findByTokenHash(tokenHash)).thenReturn(Optional.of(sesion));

        logoutService.ejecutar(new LogoutCommand(rawToken));

        assertThat(sesion.getSrfRevocado()).isTrue();
        verify(sesionRefreshRepositoryPort).save(sesion);
    }

    @Test
    @DisplayName("Debe ser idempotente si la sesion no existe o ya estaba revocada")
    void shouldBeIdempotentWhenSessionMissingOrAlreadyRevoked() {
        String missingToken = "missing-token";
        when(sesionRefreshRepositoryPort.findByTokenHash(TokenHashUtil.sha256Hex(missingToken)))
                .thenReturn(Optional.empty());

        logoutService.ejecutar(new LogoutCommand(missingToken));
        verify(sesionRefreshRepositoryPort, never()).save(any());

        String revokedToken = "already-revoked";
        String revokedHash = TokenHashUtil.sha256Hex(revokedToken);
        SesionRefresh alreadyRevoked = SesionRefresh.builder()
                .srfId(2L)
                .srfTokenHash(revokedHash)
                .srfRevocado(Boolean.TRUE)
                .build();
        when(sesionRefreshRepositoryPort.findByTokenHash(revokedHash)).thenReturn(Optional.of(alreadyRevoked));

        logoutService.ejecutar(new LogoutCommand(revokedToken));
        verify(sesionRefreshRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar 400 VALIDATION_ERROR cuando el comando o refresh token es nulo/vacio")
    void shouldThrow400WhenCommandOrTokenIsInvalid() {
        assertThatThrownBy(() -> logoutService.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getStatusCode()).isEqualTo(400));

        assertThatThrownBy(() -> logoutService.ejecutar(new LogoutCommand("  ")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getStatusCode()).isEqualTo(400));
    }
}
