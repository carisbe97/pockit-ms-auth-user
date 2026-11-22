package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {
}
