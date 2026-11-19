package com.pockit.finance.auth.security.domain.ports.out;

import com.pockit.finance.auth.users.domain.models.Usuario;

import java.util.UUID;

/**
 * Puerto de salida para emision y validacion de tokens JWT propios.
 */
public interface ITokenPort {

    String generarAccessToken(Usuario usuario);

    UUID validarYExtraerUsuarioId(String token);

    long obtenerExpiracionSegundos();
}
