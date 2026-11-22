package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.RestablecerPasswordCommand;
import com.pockit.finance.auth.users.domain.ports.in.IRestablecerPasswordUseCase;
import com.pockit.finance.auth.users.application.usecases.RestablecerPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada que envuelve el caso de uso de restablecimiento de contrasena.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalRestablecerPasswordAdapter implements IRestablecerPasswordUseCase {

    private final RestablecerPasswordService restablecerPasswordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ejecutar(RestablecerPasswordCommand command) {
        restablecerPasswordService.ejecutar(command);
    }
}
