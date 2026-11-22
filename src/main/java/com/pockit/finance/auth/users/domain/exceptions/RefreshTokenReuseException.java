package com.pockit.finance.auth.users.domain.exceptions;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;

/**
 * Excepcion lanzada cuando se detecta reutilizacion de un refresh token ya revocado.
 */
public class RefreshTokenReuseException extends BaseDomainException {

    private static final int STATUS_UNAUTHORIZED = 401;
    private static final String CODE_REFRESH_TOKEN_INVALIDO = "REFRESH_TOKEN_INVALIDO";

    public RefreshTokenReuseException() {
        this("El refresh token es invalido o ha sido revocado.");
    }

    public RefreshTokenReuseException(String message) {
        super(STATUS_UNAUTHORIZED, CODE_REFRESH_TOKEN_INVALIDO, message);
    }
}
