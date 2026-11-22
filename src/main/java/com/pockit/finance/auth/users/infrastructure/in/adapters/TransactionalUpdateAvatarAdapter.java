package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.UpdateAvatarCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IUpdateAvatarUseCase;
import com.pockit.finance.auth.users.application.usecases.UpdateAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador transaccional de entrada que envuelve el caso de uso de actualizacion de avatar.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalUpdateAvatarAdapter implements IUpdateAvatarUseCase {

    private final UpdateAvatarService updateAvatarService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Usuario ejecutar(UpdateAvatarCommand command) {
        return updateAvatarService.ejecutar(command);
    }
}
