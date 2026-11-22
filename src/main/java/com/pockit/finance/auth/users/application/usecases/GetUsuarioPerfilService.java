package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IGetUsuarioPerfilUseCase;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de dominio puro para consulta del perfil del usuario autenticado.
 */
public class GetUsuarioPerfilService implements IGetUsuarioPerfilUseCase {

    private static final String CODE_UNAUTHORIZED = "NO_AUTENTICADO";
    private static final String CODE_USUARIO_NO_ENCONTRADO = "USUARIO_NO_ENCONTRADO";

    private final IUsuarioRepositoryPort usuarioRepositoryPort;

    public GetUsuarioPerfilService(IUsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
    }

    @Override
    public Usuario ejecutar(UUID usrId) {
        if (usrId == null) {
            throw new BaseDomainException(401, CODE_UNAUTHORIZED, "Se requiere autenticacion valida para acceder a este recurso.");
        }
        return usuarioRepositoryPort.findById(usrId)
                .orElseThrow(() -> new BaseDomainException(404, CODE_USUARIO_NO_ENCONTRADO, "Usuario no encontrado."));
    }
}
