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
    public static final String CODE_IMAGEN_SUBIDA_ERROR = "IMAGEN_SUBIDA_ERROR";

    // Codigos de error de usuarios y registro (Issue #4)
    public static final String CODE_CORREO_YA_REGISTRADO = "CORREO_YA_REGISTRADO";
    public static final String CODE_CORREO_INVALIDO = "CORREO_INVALIDO";
    public static final String CODE_PASSWORD_DEBIL = "PASSWORD_DEBIL";
    public static final String CODE_EDAD_INVALIDA = "EDAD_INVALIDA";
    public static final String CODE_UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";

    // Codigos de error de autenticacion y perfil (Issue #5)
    public static final String CODE_CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS";
    public static final String CODE_REFRESH_TOKEN_INVALIDO = "REFRESH_TOKEN_INVALIDO";
    public static final String CODE_USUARIO_NO_ENCONTRADO = "USUARIO_NO_ENCONTRADO";

    // Codigos y mensajes de recuperacion de contrasena (Issue #6)
    public static final String CODE_RESET_TOKEN_INVALIDO = "RESET_TOKEN_INVALIDO";

    // Categorias de sistema (siembra al registrar usuario)
    public static final String CAT_SISTEMA_TRANSFERENCIA_SALIENTE = "Transferencia Saliente";
    public static final String CAT_SISTEMA_TRANSFERENCIA_ENTRANTE = "Transferencia Entrante";
    public static final String CAT_SISTEMA_COLOR_DEFAULT = "#64748B";
    public static final String CAT_SISTEMA_ICONO_SALIENTE = "arrow_upward";
    public static final String CAT_SISTEMA_ICONO_ENTRANTE = "arrow_downward";

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
    public static final String MSG_CREDENCIALES_INVALIDAS = "Credenciales invalidas.";
    public static final String MSG_REFRESH_TOKEN_INVALIDO = "El refresh token es invalido o ha expirado.";
    public static final String MSG_USUARIO_NO_ENCONTRADO = "Usuario no encontrado.";
    public static final String MSG_RESET_TOKEN_INVALIDO = "El token de recuperacion es invalido o ha expirado.";
    public static final String MSG_RECUPERAR_PASSWORD_OK = "Si el correo se encuentra registrado, recibiras instrucciones para restablecer tu contrasena.";
    public static final String MSG_RESTABLECER_PASSWORD_OK = "Tu contrasena ha sido restablecida exitosamente.";
}
