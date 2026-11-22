package com.pockit.finance.auth.users.infrastructure.in.adapters;

import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IGetUsuarioPerfilUseCase;
import com.pockit.finance.auth.users.application.usecases.GetUsuarioPerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Adaptador transaccional de entrada de solo lectura para consultar el perfil del usuario.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalGetUsuarioPerfilAdapter implements IGetUsuarioPerfilUseCase {

    private final GetUsuarioPerfilService getUsuarioPerfilService;

    @Override
    @Transactional(readOnly = true)
    public Usuario ejecutar(UUID usrId) {
        return getUsuarioPerfilService.ejecutar(usrId);
    }
}
