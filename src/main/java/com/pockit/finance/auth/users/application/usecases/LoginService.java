package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.EmailUtil;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.LoginCommand;
import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.ILoginUseCase;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Servicio de dominio puro para autenticacion de usuarios y emision de JWT + Refresh Token.
 */
public class LoginService implements ILoginUseCase {

    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String CODE_CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS";
    private static final String MSG_CREDENCIALES_INVALIDAS = "Credenciales invalidas.";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final IUsuarioRepositoryPort usuarioRepositoryPort;
    private final IPasswordHasherPort passwordHasherPort;
    private final ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;
    private final ITokenPort tokenPort;
    private final long refreshExpirationSeconds;
    private final Clock clock;

    public LoginService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            ITokenPort tokenPort,
            long refreshExpirationSeconds
    ) {
        this(usuarioRepositoryPort, passwordHasherPort, sesionRefreshRepositoryPort, tokenPort, refreshExpirationSeconds, Clock.systemUTC());
    }

    public LoginService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            ITokenPort tokenPort,
            long refreshExpirationSeconds,
            Clock clock
    ) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
        this.passwordHasherPort = Objects.requireNonNull(passwordHasherPort);
        this.sesionRefreshRepositoryPort = Objects.requireNonNull(sesionRefreshRepositoryPort);
        this.tokenPort = Objects.requireNonNull(tokenPort);
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AuthResult ejecutar(LoginCommand command) {
        if (command == null) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "La solicitud de autenticacion es obligatoria.");
        }
        if (command.correo() == null || command.correo().isBlank()
                || command.password() == null || command.password().isBlank()) {
            throw new BaseDomainException(401, CODE_CREDENCIALES_INVALIDAS, MSG_CREDENCIALES_INVALIDAS);
        }

        String correoNormalizado = EmailUtil.normalize(command.correo());
        Usuario usuario = usuarioRepositoryPort.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new BaseDomainException(401, CODE_CREDENCIALES_INVALIDAS, MSG_CREDENCIALES_INVALIDAS));

        if (!usuario.estaActivo()
                || !passwordHasherPort.matches(command.password(), usuario.getUsrPasswordHash())) {
            throw new BaseDomainException(401, CODE_CREDENCIALES_INVALIDAS, MSG_CREDENCIALES_INVALIDAS);
        }

        String accessToken = tokenPort.generarAccessToken(usuario);
        long expiresIn = tokenPort.obtenerExpiracionSegundos();
        String rawRefreshToken = TokenHashUtil.generateOpaqueToken();
        String tokenHash = TokenHashUtil.sha256Hex(rawRefreshToken);

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        String dispositivo = command.dispositivo() == null || command.dispositivo().isBlank()
                ? null
                : command.dispositivo().trim();

        SesionRefresh nuevaSesion = SesionRefresh.builder()
                .srfUsrId(usuario.getUsrId())
                .srfTokenHash(tokenHash)
                .srfExpiraEn(ahora.plusSeconds(refreshExpirationSeconds))
                .srfRevocado(Boolean.FALSE)
                .srfDispositivo(dispositivo)
                .srfCreadoEn(ahora)
                .build();

        sesionRefreshRepositoryPort.save(nuevaSesion);

        return new AuthResult(accessToken, rawRefreshToken, TOKEN_TYPE_BEARER, expiresIn, usuario);
    }
}
