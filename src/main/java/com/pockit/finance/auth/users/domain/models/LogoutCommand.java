package com.pockit.finance.auth.users.domain.models;

/**
 * Comando de dominio para cierre de sesion (revocacion de refresh token).
 */
public record LogoutCommand(
        String refreshToken
) {
}
