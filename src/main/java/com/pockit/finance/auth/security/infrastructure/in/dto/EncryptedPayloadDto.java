package com.pockit.finance.auth.security.infrastructure.in.dto;

public record EncryptedPayloadDto(
        String encryptedKey,
        String iv,
        String cipherText,
        Long timestamp
) {
}
