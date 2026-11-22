package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.exceptions.RefreshTokenReuseException;
import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;
import com.pockit.finance.auth.users.domain.ports.in.IRefreshTokenUseCase;
import com.pockit.finance.auth.users.application.usecases.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada para rotacion de refresh tokens.
 * Mantiene el commit de la revocacion masiva cuando se detecta reuso de token.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalRefreshTokenAdapter implements IRefreshTokenUseCase {

    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = RefreshTokenReuseException.class)
    public AuthResult ejecutar(RefreshTokenCommand command) {
        return refreshTokenService.ejecutar(command);
    }
}
