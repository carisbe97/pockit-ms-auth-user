package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.LogoutCommand;
import com.pockit.finance.auth.users.domain.ports.in.ILogoutUseCase;
import com.pockit.finance.auth.users.application.usecases.LogoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada que envuelve el caso de uso de logout.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalLogoutAdapter implements ILogoutUseCase {

    private final LogoutService logoutService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ejecutar(LogoutCommand command) {
        logoutService.ejecutar(command);
    }
}
