package com.pockit.finance.auth.security.domain.ports.out;

import com.pockit.finance.auth.security.domain.models.EncryptedPayload;

/**
 * Puerto de salida criptografico para descifrado hibrido de payloads y exposicion de llave publica.
 */
public interface ICryptoPort {

    String decryptPayload(EncryptedPayload payload);

    String getPublicKeyBase64();

    String getAlgorithm();
}
