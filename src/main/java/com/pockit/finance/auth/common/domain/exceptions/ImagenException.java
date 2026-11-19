package com.pockit.finance.auth.common.domain.exceptions;

/**
 * Excepcion especializada para validaciones de imagenes (formato, tamano <= 2MB, legibilidad).
 */
public class ImagenException extends BaseDomainException {

    public ImagenException(String code, String message) {
        super(400, code, message);
    }

    public ImagenException(int statusCode, String code, String message) {
        super(statusCode, code, message);
    }

    public ImagenException(String code, String message, Throwable cause) {
        super(400, code, message, cause);
    }
}
