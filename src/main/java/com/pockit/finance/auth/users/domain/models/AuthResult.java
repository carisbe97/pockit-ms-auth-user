package com.pockit.finance.auth.users.domain.models;

/**
 * Resultado de dominio para operaciones de autenticacion y rotacion de tokens.
 */
public record AuthResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Usuario usuario
) {
}
