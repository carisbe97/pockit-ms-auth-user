package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.ICreateUsuarioUseCase;
import com.pockit.finance.auth.users.application.usecases.CreateUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada que envuelve el caso de uso de registro de usuario.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalCreateUsuarioAdapter implements ICreateUsuarioUseCase {

    private final CreateUsuarioService createUsuarioService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Usuario ejecutar(CreateUsuarioCommand command) {
        return createUsuarioService.ejecutar(command);
    }
}
