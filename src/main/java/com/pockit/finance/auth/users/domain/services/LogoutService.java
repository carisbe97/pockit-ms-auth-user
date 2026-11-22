package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.users.domain.models.LogoutCommand;
import com.pockit.finance.auth.users.domain.ports.in.ILogoutUseCase;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;

import java.util.Objects;

/**
 * Servicio de dominio puro para cierre de sesion mediante revocacion del refresh token.
 */
public class LogoutService implements ILogoutUseCase {

    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";

    private final ISesionRefreshRepositoryPort sesionRefreshRepositoryPort;

    public LogoutService(ISesionRefreshRepositoryPort sesionRefreshRepositoryPort) {
        this.sesionRefreshRepositoryPort = Objects.requireNonNull(sesionRefreshRepositoryPort);
    }

    @Override
    public void ejecutar(LogoutCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "El refresh token es obligatorio.");
        }

        String tokenHash = TokenHashUtil.sha256Hex(command.refreshToken());
        sesionRefreshRepositoryPort.findByTokenHash(tokenHash)
                .ifPresent(sesion -> {
                    if (!Boolean.TRUE.equals(sesion.getSrfRevocado())) {
                        sesion.revocar();
                        sesionRefreshRepositoryPort.save(sesion);
                    }
                });
    }
}
