package com.pockit.finance.auth.users.domain.models;

/**
 * Comando de dominio para rotacion de refresh token.
 */
public record RefreshTokenCommand(
        String refreshToken,
        String dispositivo
) {
}
