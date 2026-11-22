package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.EmailUtil;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.users.domain.models.RecuperarPasswordCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IRecuperarPasswordUseCase;
import com.pockit.finance.auth.users.domain.ports.out.IRecoveryNotificationPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Servicio de dominio puro para solicitar recuperacion de contrasena (anti-enumeracion).
 */
public class RecuperarPasswordService implements IRecuperarPasswordUseCase {

    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";

    private final IUsuarioRepositoryPort usuarioRepositoryPort;
    private final IRecoveryNotificationPort recoveryNotificationPort;
    private final long resetExpirationMinutes;
    private final Clock clock;

    public RecuperarPasswordService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IRecoveryNotificationPort recoveryNotificationPort,
            long resetExpirationMinutes
    ) {
        this(usuarioRepositoryPort, recoveryNotificationPort, resetExpirationMinutes, Clock.systemUTC());
    }

    public RecuperarPasswordService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IRecoveryNotificationPort recoveryNotificationPort,
            long resetExpirationMinutes,
            Clock clock
    ) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
        this.recoveryNotificationPort = Objects.requireNonNull(recoveryNotificationPort);
        this.resetExpirationMinutes = resetExpirationMinutes;
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void ejecutar(RecuperarPasswordCommand command) {
        if (command == null || command.correo() == null || command.correo().isBlank()) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "El correo electronico es obligatorio.");
        }

        String correoNormalizado = EmailUtil.normalize(command.correo());
        usuarioRepositoryPort.findByCorreo(correoNormalizado)
                .filter(Usuario::estaActivo)
                .ifPresent(usuario -> {
                    String rawToken = TokenHashUtil.generateOpaqueToken();
                    String tokenHash = TokenHashUtil.sha256Hex(rawToken);
                    OffsetDateTime expiraEn = OffsetDateTime.now(clock).plusMinutes(resetExpirationMinutes);

                    usuario.asignarTokenRecuperacion(tokenHash, expiraEn);
                    usuarioRepositoryPort.save(usuario);
                    recoveryNotificationPort.enviarTokenRecuperacion(usuario.getUsrCorreo(), rawToken, expiraEn);
                });
    }
}
