package com.pockit.finance.auth.common.infrastructure.errors;

import java.util.List;

/**
 * Contrato JSON uniforme de errores HTTP para todos los microservicios de Pockit Finance.
 */
public record ErrorResponseDto(
        int statusCode,
        String code,
        String message,
        List<String> details
) {
    public ErrorResponseDto {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ErrorResponseDto of(int statusCode, String code, String message) {
        return new ErrorResponseDto(statusCode, code, message, List.of());
    }
}
