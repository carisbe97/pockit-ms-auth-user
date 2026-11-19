package com.pockit.finance.auth.security.infrastructure.out.crypto;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.models.EncryptedPayload;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
public class RsaAesGcmCryptoAdapter implements ICryptoPort {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_BYTES = 32; // 256 bits
    private static final int GCM_IV_BYTES = 12;  // 96 bits
    private static final int GCM_TAG_BITS = 128; // 16 bytes
    private static final int GCM_TAG_BYTES = 16;

    private final KeyPair keyPair;
    private final long replayWindowSeconds;
    private final Clock clock;

    @Autowired
    public RsaAesGcmCryptoAdapter(
            @Value("${app.crypto.rsa.private-key:}") String privateKeyBase64,
            @Value("${app.crypto.rsa.public-key:}") String publicKeyBase64,
            @Value("${app.crypto.replay-window-seconds:300}") long replayWindowSeconds
    ) {
        this(resolveKeyPair(privateKeyBase64, publicKeyBase64), replayWindowSeconds, Clock.systemUTC());
    }

    RsaAesGcmCryptoAdapter(KeyPair keyPair, long replayWindowSeconds, Clock clock) {
        this.keyPair = keyPair;
        this.replayWindowSeconds = replayWindowSeconds;
        this.clock = clock;
    }

    @Override
    public String decryptPayload(EncryptedPayload payload) {
        if (payload == null
                || isBlank(payload.encryptedKey())
                || isBlank(payload.iv())
                || isBlank(payload.cipherText())) {
            throw new BaseDomainException(
                    400,
                    BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                    BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
            );
        }

        // Validar ventana anti-replay antes de ejecutar RSA costoso
        long nowSeconds = Instant.now(clock).getEpochSecond();
        if (Math.abs(nowSeconds - payload.timestamp()) > replayWindowSeconds) {
            throw new BaseDomainException(
                    400,
                    BaseConstants.CODE_PAYLOAD_EXPIRADO,
                    BaseConstants.MSG_PAYLOAD_EXPIRADO
            );
        }

        try {
            byte[] encryptedAesKey = Base64.getDecoder().decode(payload.encryptedKey());
            byte[] ivBytes = Base64.getDecoder().decode(payload.iv());
            byte[] cipherBytes = Base64.getDecoder().decode(payload.cipherText());

            if (ivBytes.length != GCM_IV_BYTES || cipherBytes.length <= GCM_TAG_BYTES) {
                throw new BaseDomainException(
                        400,
                        BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                        BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
                );
            }

            Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
            OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );
            rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), oaepSpec);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

            if (aesKeyBytes.length != AES_KEY_BYTES) {
                throw new BaseDomainException(
                        400,
                        BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                        BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
                );
            }

            Cipher aesCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            aesCipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKeyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, ivBytes)
            );
            byte[] plainBytes = aesCipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (BaseDomainException ex) {
            throw ex;
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            log.warn("Fallo al descifrar payload hibrido RSA/AES-GCM: {}", ex.getClass().getSimpleName());
            throw new BaseDomainException(
                    400,
                    BaseConstants.CODE_PAYLOAD_INTEGRIDAD_INVALIDA,
                    BaseConstants.MSG_PAYLOAD_INTEGRIDAD_INVALIDA
            );
        }
    }

    @Override
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @Override
    public String getAlgorithm() {
        return BaseConstants.RSA_OAEP_ALGORITHM;
    }

    private static KeyPair resolveKeyPair(String privateKeyBase64, String publicKeyBase64) {
        if (!isBlank(privateKeyBase64) && !isBlank(publicKeyBase64)) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] privBytes = Base64.getDecoder().decode(privateKeyBase64.trim());
                byte[] pubBytes = Base64.getDecoder().decode(publicKeyBase64.trim());
                PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
                return new KeyPair(publicKey, privateKey);
            } catch (GeneralSecurityException | IllegalArgumentException ex) {
                throw new IllegalStateException("Las llaves RSA configuradas son invalidas.", ex);
            }
        }
        try {
            log.warn("Generando KeyPair RSA-2048 efimero en memoria (configure app.crypto.rsa.* en produccion).");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No fue posible inicializar el generador RSA-2048.", ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
