package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequestDto(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken,

        @Size(max = 150, message = "El dispositivo no puede superar 150 caracteres")
        String dispositivo
) {
}
