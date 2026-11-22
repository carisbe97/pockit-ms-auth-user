package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo es invalido")
        String usrCorreo,

        @NotBlank(message = "La contrasena es obligatoria")
        String usrPassword,

        @Size(max = 150, message = "El dispositivo no puede superar 150 caracteres")
        String dispositivo
) {
}
