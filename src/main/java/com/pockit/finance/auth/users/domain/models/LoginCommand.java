package com.pockit.finance.auth.users.domain.models;

/**
 * Comando de dominio para autenticacion de usuario.
 */
public record LoginCommand(
        String correo,
        String password,
        String dispositivo
) {
}
