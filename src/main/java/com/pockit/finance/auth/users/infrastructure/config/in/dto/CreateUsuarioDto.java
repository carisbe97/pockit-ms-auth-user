package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registro de usuario.
 */
public record CreateUsuarioDto(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato valido")
        @Size(max = 255, message = "El correo no puede exceder 255 caracteres")
        String usrCorreo,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 128, message = "La contrasena debe tener entre 8 y 128 caracteres")
        String usrPassword,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String usrNombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String usrApellido,

        @NotNull(message = "La edad es obligatoria")
        @Min(value = 0, message = "La edad minima es 0")
        @Max(value = 130, message = "La edad maxima es 130")
        Short usrEdad
) {
}
