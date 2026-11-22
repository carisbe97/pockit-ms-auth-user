package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.users.domain.models.UpdateAvatarCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.in.IUpdateAvatarUseCase;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;

import java.util.Objects;

/**
 * Servicio de dominio puro para actualizar la imagen de avatar del usuario autenticado.
 */
public class UpdateAvatarService implements IUpdateAvatarUseCase {

    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String CODE_UNAUTHORIZED = "NO_AUTENTICADO";
    private static final String CODE_IMAGEN_VACIA = "IMAGEN_VACIA";
    private static final String CODE_USUARIO_NO_ENCONTRADO = "USUARIO_NO_ENCONTRADO";

    private final IUsuarioRepositoryPort usuarioRepositoryPort;
    private final IStoragePort storagePort;

    public UpdateAvatarService(
            IUsuarioRepositoryPort usuarioRepositoryPort,
            IStoragePort storagePort
    ) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort);
        this.storagePort = Objects.requireNonNull(storagePort);
    }

    @Override
    public Usuario ejecutar(UpdateAvatarCommand command) {
        if (command == null) {
            throw new BaseDomainException(400, CODE_VALIDATION_ERROR, "La solicitud de actualizacion de avatar es obligatoria.");
        }
        if (command.usrId() == null) {
            throw new BaseDomainException(401, CODE_UNAUTHORIZED, "Se requiere autenticacion valida para acceder a este recurso.");
        }
        if (command.avatarBytes() == null || command.avatarBytes().length == 0) {
            throw new BaseDomainException(400, CODE_IMAGEN_VACIA, "La imagen no puede estar vacia.");
        }

        Usuario usuario = usuarioRepositoryPort.findById(command.usrId())
                .orElseThrow(() -> new BaseDomainException(404, CODE_USUARIO_NO_ENCONTRADO, "Usuario no encontrado."));

        String nuevaUrl = storagePort.subirAvatar(
                usuario.getUsrId(),
                command.avatarBytes(),
                command.avatarContentType(),
                command.avatarExtension()
        );
        usuario.actualizarAvatar(nuevaUrl);
        return usuarioRepositoryPort.save(usuario);
    }
}
