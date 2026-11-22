package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.LoginCommand;
import com.pockit.finance.auth.users.domain.ports.in.ILoginUseCase;
import com.pockit.finance.auth.users.application.usecases.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada que envuelve el caso de uso de login.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalLoginAdapter implements ILoginUseCase {

    private final LoginService loginService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResult ejecutar(LoginCommand command) {
        return loginService.ejecutar(command);
    }
}
