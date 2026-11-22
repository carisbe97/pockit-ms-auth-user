package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.users.domain.exceptions.RefreshTokenReuseException;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;
import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IRefreshTokenUseCase;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Servicio de dominio puro para rotacion de refresh tokens con deteccion de reuso.
 */
public class RefreshTokenService implements IRefreshTokenUseCase {

    private static final String CODE_REFRESH_TOKEN_INVALIDO = "REFRESH_TOKEN_INVALIDO";
    private static final String MSG_REFRESH_TOKEN_INVALIDO = "El refresh token es invalido o ha expirado.";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;
    private final IUsuarioRepositoryPort usuarioRepositoryPort;
    private final ITokenPort tokenPort;
    private final long refreshExpirationSeconds;
    private final Clock clock;

    public RefreshTokenService(
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            IUsuarioRepositoryPort usuarioRepositoryPort,
            ITokenPort tokenPort,
            long refreshExpirationSeconds
    ) {
        this(sesionRefreshRepositoryPort, usuarioRepositoryPort, tokenPort, refreshExpirationSeconds, Clock.systemUTC());
    }

    public RefreshTokenService(
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            IUsuarioRepositoryPort usuarioRepositoryPort,
            ITokenPort tokenPort,
            long refreshExpirationSeconds,
            Clock clock
    ) {
        this.sesionRefreshRepositoryPort = Objects.requireNonNull(sesionRefreshRepositoryPort);
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
        this.tokenPort = Objects.requireNonNull(tokenPort);
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AuthResult ejecutar(RefreshTokenCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new BaseDomainException(401, CODE_REFRESH_TOKEN_INVALIDO, MSG_REFRESH_TOKEN_INVALIDO);
        }

        String tokenHash = TokenHashUtil.sha256Hex(command.refreshToken());
        SesionRefresh sesionActual = sesionRefreshRepositoryPort.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BaseDomainException(401, CODE_REFRESH_TOKEN_INVALIDO, MSG_REFRESH_TOKEN_INVALIDO));

        if (Boolean.TRUE.equals(sesionActual.getSrfRevocado())) {
            sesionRefreshRepositoryPort.revocarTodasPorUsuario(sesionActual.getSrfUsrId());
            throw new RefreshTokenReuseException(
                    "Se detecto reutilizacion de refresh token. Todas las sesiones han sido revocadas."
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if (!sesionActual.esValida(ahora)) {
            throw new BaseDomainException(401, CODE_REFRESH_TOKEN_INVALIDO, MSG_REFRESH_TOKEN_INVALIDO);
        }

        Usuario usuario = usuarioRepositoryPort.findById(sesionActual.getSrfUsrId())
                .orElseThrow(() -> new BaseDomainException(401, CODE_REFRESH_TOKEN_INVALIDO, MSG_REFRESH_TOKEN_INVALIDO));

        if (!usuario.estaActivo()) {
            sesionRefreshRepositoryPort.revocarTodasPorUsuario(usuario.getUsrId());
            throw new BaseDomainException(401, CODE_REFRESH_TOKEN_INVALIDO, MSG_REFRESH_TOKEN_INVALIDO);
        }

        sesionActual.revocar();
        sesionRefreshRepositoryPort.save(sesionActual);

        String nuevoAccessToken = tokenPort.generarAccessToken(usuario);
        long expiresIn = tokenPort.obtenerExpiracionSegundos();
        String nuevoRawRefreshToken = TokenHashUtil.generateOpaqueToken();
        String nuevoTokenHash = TokenHashUtil.sha256Hex(nuevoRawRefreshToken);

        String dispositivo = command.dispositivo() == null || command.dispositivo().isBlank()
                ? sesionActual.getSrfDispositivo()
                : command.dispositivo().trim();

        SesionRefresh nuevaSesion = SesionRefresh.builder()
                .srfUsrId(usuario.getUsrId())
                .srfTokenHash(nuevoTokenHash)
                .srfExpiraEn(ahora.plusSeconds(refreshExpirationSeconds))
                .srfRevocado(Boolean.FALSE)
                .srfDispositivo(dispositivo)
                .srfCreadoEn(ahora)
                .build();

        sesionRefreshRepositoryPort.save(nuevaSesion);

        return new AuthResult(nuevoAccessToken, nuevoRawRefreshToken, TOKEN_TYPE_BEARER, expiresIn, usuario);
    }
}
