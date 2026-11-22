package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.LoginCommand;
import com.pockit.finance.auth.users.domain.models.LogoutCommand;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.services.GetUsuarioPerfilService;
import com.pockit.finance.auth.users.domain.services.LoginService;
import com.pockit.finance.auth.users.domain.services.LogoutService;
import com.pockit.finance.auth.users.domain.services.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalAuthAdaptersTest {

    @Mock
    private LoginService loginService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LogoutService logoutService;
    @Mock
    private GetUsuarioPerfilService getUsuarioPerfilService;

    @InjectMocks
    private TransactionalLoginAdapter loginAdapter;
    @InjectMocks
    private TransactionalRefreshTokenAdapter refreshTokenAdapter;
    @InjectMocks
    private TransactionalLogoutAdapter logoutAdapter;
    @InjectMocks
    private TransactionalGetUsuarioPerfilAdapter getUsuarioPerfilAdapter;

    @Test
    @DisplayName("TransactionalLoginAdapter debe delegar a LoginService")
    void shouldDelegateLogin() {
        LoginCommand cmd = new LoginCommand("user@pockit.com", "Pockit@2026", "Web");
        AuthResult expected = new AuthResult("access", "refresh", "Bearer", 900L, Usuario.builder().build());
        when(loginService.ejecutar(cmd)).thenReturn(expected);

        AuthResult actual = loginAdapter.ejecutar(cmd);

        assertThat(actual).isSameAs(expected);
        verify(loginService).ejecutar(cmd);
    }

    @Test
    @DisplayName("TransactionalRefreshTokenAdapter debe delegar a RefreshTokenService")
    void shouldDelegateRefreshToken() {
        RefreshTokenCommand cmd = new RefreshTokenCommand("refresh-raw", "Mobile");
        AuthResult expected = new AuthResult("access2", "refresh2", "Bearer", 900L, Usuario.builder().build());
        when(refreshTokenService.ejecutar(cmd)).thenReturn(expected);

        AuthResult actual = refreshTokenAdapter.ejecutar(cmd);

        assertThat(actual).isSameAs(expected);
        verify(refreshTokenService).ejecutar(cmd);
    }

    @Test
    @DisplayName("TransactionalLogoutAdapter debe delegar a LogoutService")
    void shouldDelegateLogout() {
        LogoutCommand cmd = new LogoutCommand("refresh-raw");

        logoutAdapter.ejecutar(cmd);

        verify(logoutService).ejecutar(cmd);
    }

    @Test
    @DisplayName("TransactionalGetUsuarioPerfilAdapter debe delegar a GetUsuarioPerfilService")
    void shouldDelegateGetUsuarioPerfil() {
        UUID usrId = UUID.randomUUID();
        Usuario expected = Usuario.builder().usrId(usrId).build();
        when(getUsuarioPerfilService.ejecutar(usrId)).thenReturn(expected);

        Usuario actual = getUsuarioPerfilAdapter.ejecutar(usrId);

        assertThat(actual).isSameAs(expected);
        verify(getUsuarioPerfilService).ejecutar(usrId);
    }
}
