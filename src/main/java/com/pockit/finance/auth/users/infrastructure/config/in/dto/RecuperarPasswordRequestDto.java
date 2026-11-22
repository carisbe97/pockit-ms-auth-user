package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarPasswordRequestDto(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo es invalido")
        String usrCorreo
) {
}
