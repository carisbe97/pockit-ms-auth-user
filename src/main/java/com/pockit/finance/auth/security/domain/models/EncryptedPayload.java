package com.pockit.finance.auth.security.domain.models;

/**
 * Modelo de dominio puro que representa un sobre de payload cifrado hibrido (AES-256-GCM + RSA-2048 OAEP).
 */
public record EncryptedPayload(
        String encryptedKey,
        String iv,
        String cipherText,
        long timestamp
) {
}
