package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.RecuperarPasswordCommand;
import com.pockit.finance.auth.users.domain.ports.in.IRecuperarPasswordUseCase;
import com.pockit.finance.auth.users.application.usecases.RecuperarPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada que envuelve el caso de uso de recuperacion de contrasena.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalRecuperarPasswordAdapter implements IRecuperarPasswordUseCase {

    private final RecuperarPasswordService recuperarPasswordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ejecutar(RecuperarPasswordCommand command) {
        recuperarPasswordService.ejecutar(command);
    }
}
