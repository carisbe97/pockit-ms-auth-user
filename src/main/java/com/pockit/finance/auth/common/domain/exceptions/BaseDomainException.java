package com.pockit.finance.auth.common.domain.exceptions;

import java.util.List;

/**
 * Excepcion base del dominio (100% pura, sin dependencias de Spring ni JPA).
 */
public class BaseDomainException extends RuntimeException {

    private final int statusCode;
    private final String code;
    private final List<String> details;

    public BaseDomainException(int statusCode, String code, String message) {
        this(statusCode, code, message, List.of(), null);
    }

    public BaseDomainException(int statusCode, String code, String message, List<String> details) {
        this(statusCode, code, message, details, null);
    }

    public BaseDomainException(int statusCode, String code, String message, Throwable cause) {
        this(statusCode, code, message, List.of(), cause);
    }

    public BaseDomainException(int statusCode, String code, String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
