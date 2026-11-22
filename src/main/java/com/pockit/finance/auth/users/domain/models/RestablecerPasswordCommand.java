package com.pockit.finance.auth.users.domain.models;

/**
 * Comando de dominio puro para restablecer la contrasena usando un token de recuperacion.
 */
public record RestablecerPasswordCommand(
        String token,
        String nuevoPassword
) {
}
