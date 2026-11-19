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

    // Codigos de error de seguridad y criptografia (Issues #2 y #3)
    public static final String CODE_PAYLOAD_EXPIRADO = "PAYLOAD_EXPIRADO";
    public static final String CODE_PAYLOAD_INTEGRIDAD_INVALIDA = "PAYLOAD_INTEGRIDAD_INVALIDA";
    public static final String CODE_TOKEN_EXPIRADO = "TOKEN_EXPIRADO";
    public static final String CODE_TOKEN_INVALIDO = "TOKEN_INVALIDO";
    public static final String CODE_UNAUTHORIZED = "NO_AUTENTICADO";
    public static final String CODE_FORBIDDEN = "ACCESO_DENEGADO";

    public static final String HEADER_PAYLOAD_ENCRYPTED = "X-Payload-Encrypted";
    public static final String RSA_OAEP_ALGORITHM = "RSA-2048-OAEP-SHA256";

    public static final String MSG_INTERNAL_ERROR = "Ocurrio un error interno inesperado. Contacte al administrador.";
    public static final String MSG_MALFORMED_JSON = "El cuerpo de la solicitud JSON tiene un formato invalido.";
    public static final String MSG_VALIDATION_ERROR = "Error de validacion en los datos enviados.";
    public static final String MSG_MAX_UPLOAD_EXCEEDED = "El archivo excede el tamano maximo permitido de 2MB.";
    public static final String MSG_PAYLOAD_EXPIRADO = "El payload cifrado ha expirado o su timestamp esta fuera de ventana.";
    public static final String MSG_PAYLOAD_INTEGRIDAD_INVALIDA = "No fue posible verificar o descifrar el payload enviado.";
    public static final String MSG_TOKEN_EXPIRADO = "El token de acceso ha expirado.";
    public static final String MSG_TOKEN_INVALIDO = "El token de acceso es invalido.";
    public static final String MSG_UNAUTHORIZED = "Se requiere autenticacion valida para acceder a este recurso.";
    public static final String MSG_FORBIDDEN = "No tiene permisos para realizar esta operacion.";
}
