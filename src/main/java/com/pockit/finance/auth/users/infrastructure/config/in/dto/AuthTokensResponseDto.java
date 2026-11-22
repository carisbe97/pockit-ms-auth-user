package com.pockit.finance.auth.users.infrastructure.config.in.dto;

public record AuthTokensResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UsuarioResponseDto usuario
) {
}
