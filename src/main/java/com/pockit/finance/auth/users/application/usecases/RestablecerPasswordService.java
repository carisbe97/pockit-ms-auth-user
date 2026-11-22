package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.PasswordUtil;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.users.domain.models.RestablecerPasswordCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IRestablecerPasswordUseCase;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Servicio de dominio puro para restablecimiento de contrasena mediante token opaco de un solo uso.
 */
public class RestablecerPasswordService implements IRestablecerPasswordUseCase {

    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String CODE_RESET_TOKEN_INVALIDO = "RESET_TOKEN_INVALIDO";
    private static final String CODE_PASSWORD_DEBIL = "PASSWORD_DEBIL";
    private static final String MSG_RESET_TOKEN_INVALIDO = "El token de recuperacion es invalido o ha expirado.";

    private final IUsuarioRepositoryPort usuarioRepositoryPort;
    private final IPasswordHasherPort passwordHasherPort;
    private final ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;
    private final Clock clock;

    public RestablecerPasswordService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort
    ) {
        this(usuarioRepositoryPort, passwordHasherPort, sesionRefreshRepositoryPort, Clock.systemUTC());
    }

    public RestablecerPasswordService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IPasswordHasherPort passwordHasherPort,
            ISesionRefreshRepositoryPort sesionRefreshRepositoryPort,
            Clock clock
    ) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
        this.passwordHasherPort = Objects.requireNonNull(passwordHasherPort);
        this.sesionRefreshRepositoryPort = Objects.requireNonNull(sesionRefreshRepositoryPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void ejecutar(RestablecerPasswordCommand command) {
        if (command == null) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "La solicitud de restablecimiento es obligatoria.");
        }
        if (command.token() == null || command.token().isBlank()) {
            throw new BaseDomainException(400, CODE_RESET_TOKEN_INVALIDO, MSG_RESET_TOKEN_INVALIDO);
        }
        if (!PasswordUtil.isStrong(command.nuevoPassword())) {
            throw new BaseDomainException(
                    422,
                    CODE_PASSWORD_DEBIL,
                    "La contrasena debe tener al menos 8 caracteres, mayuscula, minuscula, numero y caracter especial."
            );
        }

        String tokenHash = TokenHashUtil.sha256Hex(command.token());
        Usuario usuario = usuarioRepositoryPort.findByResetTokenHash(tokenHash)
                .orElseThrow(() -> new BaseDomainException(400, CODE_RESET_TOKEN_INVALIDO, MSG_RESET_TOKEN_INVALIDO));

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if (!usuario.estaActivo() || !usuario.tokenRecuperacionValido(ahora)) {
            throw new BaseDomainException(400, CODE_RESET_TOKEN_INVALIDO, MSG_RESET_TOKEN_INVALIDO);
        }

        String nuevoPasswordHash = passwordHasherPort.hash(command.nuevoPassword());
        usuario.restablecerPassword(nuevoPasswordHash);
        usuarioRepositoryPort.save(usuario);
        sesionRefreshRepositoryPort.revocarTodasPorUsuario(usuario.getUsrId());
    }
}
