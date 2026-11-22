package com.pockit.finance.auth.users.domain.models;

import java.util.UUID;

/**
 * Comando de dominio puro para actualizar el avatar del usuario autenticado.
 */
public record UpdateAvatarCommand(
        UUID usrId,
        byte[] avatarBytes,
        String avatarContentType,
        String avatarExtension
) {
}
