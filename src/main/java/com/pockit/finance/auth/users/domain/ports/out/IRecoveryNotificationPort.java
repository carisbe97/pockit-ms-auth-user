package com.pockit.finance.auth.users.domain.ports.out;

import java.time.OffsetDateTime;

/**
 * Puerto de salida para notificar tokens de recuperacion de contrasena al usuario.
 */
public interface IRecoveryNotificationPort {

    void enviarTokenRecuperacion(String correoDestino, String rawToken, OffsetDateTime expiraEn);
}
