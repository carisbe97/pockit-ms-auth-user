package com.pockit.finance.auth.users.domain.models;

/**
 * Comando de dominio puro para el registro de un nuevo usuario.
 */
public record CreateUsuarioCommand(
        String correo,
        String password,
        String nombre,
        String apellido,
        Short edad,
        byte[] avatarBytes,
        String avatarContentType,
        String avatarExtension
) {
}
