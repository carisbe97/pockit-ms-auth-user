package com.pockit.finance.auth.users.infrastructure.out.notifications;

import com.pockit.finance.auth.users.domain.ports.out.IRecoveryNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Adaptador de salida para notificacion de recuperacion de contrasena mediante logging estructurado.
 * Nunca registra el token en texto claro en los logs.
 */
@Slf4j
@Component
public class LoggingRecoveryNotificationAdapter implements IRecoveryNotificationPort {

    @Override
    public void enviarTokenRecuperacion(String correoDestino, String rawToken, OffsetDateTime expiraEn) {
        log.info("Solicitud de recuperacion de contrasena procesada para el correo {} con expiracion en {}.",
                correoDestino, expiraEn);
    }
}
