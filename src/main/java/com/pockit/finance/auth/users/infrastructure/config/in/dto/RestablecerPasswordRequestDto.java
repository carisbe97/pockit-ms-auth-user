package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import jakarta.validation.constraints.NotBlank;

public record RestablecerPasswordRequestDto(
        @NotBlank(message = "El token de recuperacion es obligatorio")
        String token,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        String nuevoPassword
) {
}
