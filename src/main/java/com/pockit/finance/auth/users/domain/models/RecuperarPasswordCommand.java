package com.pockit.finance.auth.users.domain.models;

/**
 * Comando de dominio puro para solicitar recuperacion de contrasena.
 */
public record RecuperarPasswordCommand(
        String correo
) {
}
