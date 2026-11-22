package com.pockit.finance.auth.users.infrastructure.config;

import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.users.domain.ports.out.ICategoriaSistemaPort;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.IRecoveryNotificationPort;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import com.pockit.finance.auth.users.domain.services.CreateUsuarioService;
import com.pockit.finance.auth.users.domain.services.GetUsuarioPerfilService;
import com.pockit.finance.auth.users.domain.services.LoginService;
import com.pockit.finance.auth.users.domain.services.LogoutService;
import com.pockit.finance.auth.users.domain.services.RecuperarPasswordService;
import com.pockit.finance.auth.users.domain.services.RefreshTokenService;
import com.pockit.finance.auth.users.domain.services.RestablecerPasswordService;
import com.pockit.finance.auth.users.domain.services.UpdateAvatarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * Configuracion de wiring de beans del modulo users.
 */
@Configuration
public class UsuarioConfig {

    @Bean
    public CreateUsuarioService createUsuarioService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ICategoriaSistemaPort categoriaSistemaPort,
            IStoragePort storagePort
    ) {
        return new CreateUsuarioService(
                usuarioRepositoryPort,
                passwordHasherPort,
                categoriaSistemaPort,
                storagePort
        );
    }

    @Bean
    public LoginService loginService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            ITokenPort tokenPort,
            @Value("${app.security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        return new LoginService(
                usuarioRepositoryPort,
                passwordHasherPort,
                sesionRefreshRepositoryPort,
                tokenPort,
                refreshExpirationSeconds,
                Clock.systemUTC()
        );
    }

    @Bean
    public RefreshTokenService refreshTokenService(
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            IUsuarioRepositoryPort usuarioRepositoryPort,
            ITokenPort tokenPort,
            @Value("${app.security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        return new RefreshTokenService(
                sesionRefreshRepositoryPort,
                usuarioRepositoryPort,
                tokenPort,
                refreshExpirationSeconds,
                Clock.systemUTC()
        );
    }

    @Bean
    public LogoutService logoutService(ISesionRefreshRepositoryPort sesionRefreshRepositoryPort) {
        return new LogoutService(sesionRefreshRepositoryPort);
    }

    @Bean
    public GetUsuarioPerfilService getUsuarioPerfilService(IUsuarioRepositoryPort usuarioRepositoryPort) {
        return new GetUsuarioPerfilService(usuarioRepositoryPort);
    }

    @Bean
    public RecuperarPasswordService recuperarPasswordService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IRecoveryNotificationPort recoveryNotificationPort,
            @Value("${app.security.reset-password.expiration-minutes:15}") long resetExpirationMinutes
    ) {
        return new RecuperarPasswordService(
                usuarioRepositoryPort,
                recoveryNotificationPort,
                resetExpirationMinutes,
                Clock.systemUTC()
        );
    }

    @Bean
    public RestablecerPasswordService restablecerPasswordService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort
    ) {
        return new RestablecerPasswordService(
                usuarioRepositoryPort,
                passwordHasherPort,
                sesionRefreshRepositoryPort,
                Clock.systemUTC()
        );
    }

    @Bean
    public UpdateAvatarService updateAvatarService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IStoragePort storagePort
    ) {
        return new UpdateAvatarService(usuarioRepositoryPort, storagePort);
    }

    @Bean
    public RestClient supabaseRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
