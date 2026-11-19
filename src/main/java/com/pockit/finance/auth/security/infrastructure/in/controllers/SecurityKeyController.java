package com.pockit.finance.auth.security.infrastructure.in.controllers;

import com.pockit.finance.auth.security.domain.ports.out.ICryptoPort;
import com.pockit.finance.auth.security.infrastructure.in.dto.PublicKeyResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Tag(name = "Seguridad", description = "Endpoints criptograficos publicos para cifrado hibrido de payloads")
public class SecurityKeyController {

    private final ICryptoPort cryptoPort;

    @Operation(
            summary = "Obtener llave publica RSA-2048",
            description = "Retorna la llave publica RSA en Base64 (X.509) para cifrar llaves efimeras AES-256-GCM desde el cliente Flutter."
    )
    @ApiResponse(responseCode = "200", description = "Llave publica RSA obtenida exitosamente")
    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponseDto> getPublicKey() {
        PublicKeyResponseDto response = new PublicKeyResponseDto(
                cryptoPort.getAlgorithm(),
                cryptoPort.getPublicKeyBase64()
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
