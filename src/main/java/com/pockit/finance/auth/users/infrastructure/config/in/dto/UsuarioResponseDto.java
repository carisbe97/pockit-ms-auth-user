package com.pockit.finance.auth.users.infrastructure.config.in.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de salida de Usuario (nunca expone usrPasswordHash ni usrResetTokenHash).
 */
public record UsuarioResponseDto(
        UUID usrId,
        String usrCorreo,
        String usrNombre,
        String usrApellido,
        Short usrEdad,
        String usrAvatarUrl,
        Boolean usrActivo,
        OffsetDateTime usrCreadoEn
) {
}
