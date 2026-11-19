package com.pockit.finance.auth.security.infrastructure.in.dto;

public record PublicKeyResponseDto(
        String algorithm,
        String publicKey
) {
}
