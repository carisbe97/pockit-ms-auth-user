package com.pockit.finance.auth.common.infrastructure.utilities;

import java.util.Set;

public final class BaseConstants {

    private BaseConstants() {
    }

    // Limites de imagenes (herencia ccts-backend)
    public static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    public static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // Codigos de error estandar
    public static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CODE_CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String CODE_MALFORMED_JSON = "MALFORMED_JSON";
    public static final String CODE_INVALID_PARAMETER_TYPE = "INVALID_PARAMETER_TYPE";
    public static final String CODE_MISSING_PARAMETER = "MISSING_PARAMETER";
    public static final String CODE_RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    public static final String CODE_IMAGEN_VACIA = "IMAGEN_VACIA";
    public static final String CODE_IMAGEN_FORMATO_INVALIDO = "IMAGEN_FORMATO_INVALIDO";
    public static final String CODE_IMAGEN_TAMANO_EXCEDIDO = "IMAGEN_TAMANO_EXCEDIDO";
    public static final String CODE_IMAGEN_CORRUPTA = "IMAGEN_CORRUPTA";

    public static final String MSG_INTERNAL_ERROR = "Ocurrio un error interno inesperado. Contacte al administrador.";
    public static final String MSG_MALFORMED_JSON = "El cuerpo de la solicitud JSON tiene un formato invalido.";
    public static final String MSG_VALIDATION_ERROR = "Error de validacion en los datos enviados.";
    public static final String MSG_MAX_UPLOAD_EXCEEDED = "El archivo excede el tamano maximo permitido de 2MB.";
}
