package com.pockit.finance.auth.security.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pockit.finance.auth.common.infrastructure.errors.BaseControllerAdvice;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import com.pockit.finance.auth.security.domain.ports.out.ITokenPort;
import com.pockit.finance.auth.security.infrastructure.config.SecurityConfig;
import com.pockit.finance.auth.security.infrastructure.config.WebMvcConfig;
import com.pockit.finance.auth.security.infrastructure.in.annotations.CurrentUserId;
import com.pockit.finance.auth.security.infrastructure.in.controllers.SecurityKeyController;
import com.pockit.finance.auth.security.infrastructure.in.dto.EncryptedPayloadDto;
import com.pockit.finance.auth.security.infrastructure.in.filters.JwtAuthenticationFilter;
import com.pockit.finance.auth.security.infrastructure.in.filters.PayloadEncryptionFilter;
import com.pockit.finance.auth.security.infrastructure.in.handlers.CustomAccessDeniedHandler;
import com.pockit.finance.auth.security.infrastructure.in.handlers.CustomAuthenticationEntryPoint;
import com.pockit.finance.auth.security.infrastructure.in.handlers.SecurityErrorResponseWriter;
import com.pockit.finance.auth.security.infrastructure.in.resolvers.CurrentUserIdArgumentResolver;
import com.pockit.finance.auth.security.infrastructure.out.crypto.RsaAesGcmCryptoAdapter;
import com.pockit.finance.auth.security.infrastructure.out.jwt.JwtTokenAdapter;
import com.pockit.finance.auth.users.domain.models.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        SecurityKeyController.class,
        SecurityFilterChainIntegrationTest.TestSecuredController.class
})
@Import({
        SecurityConfig.class,
        WebMvcConfig.class,
        BaseControllerAdvice.class,
        SecurityFilterChainIntegrationTest.TestSecuredController.class,
        RsaAesGcmCryptoAdapter.class,
        JwtTokenAdapter.class,
        PayloadEncryptionFilter.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CurrentUserIdArgumentResolver.class
})
@TestPropertySource(properties = {
        "app.security.jwt.secret=pockit-test-jwt-secret-key-minimo-64-bytes-para-hs512-0123456789abcdef",
        "app.security.jwt.issuer=pockit-ms-auth-user",
        "app.security.jwt.expiration-seconds=900",
        "app.crypto.replay-window-seconds=300",
        "app.security.cors.allowed-origins=http://localhost:4200"
})
class SecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ITokenPort tokenPort;

    @Autowired
    private ICryptoPort cryptoPort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Ruta publica /api/security/public-key accesible sin JWT y con cabeceras de seguridad")
    void shouldAllowPublicKeyAndIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/security/public-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(jsonPath("$.algorithm").value(BaseConstants.RSA_OAEP_ALGORITHM));
    }

    @Test
    @DisplayName("Ruta protegida sin JWT retorna 401 uniforme y con JWT valido resuelve @CurrentUserId")
    void shouldProtectPrivateRoutesAndResolveCurrentUserId() throws Exception {
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_UNAUTHORIZED));

        UUID usrId = UUID.randomUUID();
        String jwt = tokenPort.generarAccessToken(Usuario.builder().usrId(usrId).usrCorreo("camilo@pockit.com").build());

        mockMvc.perform(get("/api/test/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usrId").value(usrId.toString()));

        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_FORBIDDEN));
    }

    @Test
    @DisplayName("Flujo completo: payload cifrado AES-GCM + RSA en ruta publica llega descifrado a @Valid @RequestBody")
    void shouldDecryptPayloadBeforeControllerValidation() throws Exception {
        byte[] pubBytes = Base64.getDecoder().decode(cryptoPort.getPublicKeyBase64());
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubBytes));

        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(256);
        SecretKey aesKey = aesGen.generateKey();

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        String plainJson = "{\"mensaje\":\"Hola Pockit Encriptado\"}";
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] cipherBytes = aesCipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                publicKey,
                new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)
        );
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        EncryptedPayloadDto envelope = new EncryptedPayloadDto(
                Base64.getEncoder().encodeToString(encryptedAesKey),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(cipherBytes),
                Instant.now().getEpochSecond()
        );

        mockMvc.perform(post("/api/auth/login")
                        .header(BaseConstants.HEADER_PAYLOAD_ENCRYPTED, "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recibido").value("Hola Pockit Encriptado"));

        assertThat(passwordEncoder.matches("Secret123*", passwordEncoder.encode("Secret123*"))).isTrue();
    }

    record EchoRequest(@NotBlank String mensaje) {
    }

    @RestController
    static class TestSecuredController {

        @GetMapping("/api/test/me")
        public Map<String, String> me(@CurrentUserId UUID usrId) {
            return Map.of("usrId", usrId.toString());
        }

        @GetMapping("/api/test/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public Map<String, String> adminOnly() {
            return Map.of("status", "ok");
        }

        @PostMapping("/api/auth/login")
        public Map<String, String> loginEcho(@Valid @RequestBody EchoRequest request) {
            return Map.of("recibido", request.mensaje());
        }
    }
}
