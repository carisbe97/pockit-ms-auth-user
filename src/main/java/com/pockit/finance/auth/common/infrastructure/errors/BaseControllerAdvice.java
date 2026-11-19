package com.pockit.finance.auth.common.infrastructure.errors;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class BaseControllerAdvice {

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ErrorResponseDto> handleBaseDomainException(BaseDomainException ex) {
        log.warn("Domain exception [{}]: {}", ex.getCode(), ex.getMessage());
        ErrorResponseDto body = new ErrorResponseDto(
                ex.getStatusCode(),
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(err -> err.getField()))
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        log.warn("Validation failed: {}", details);
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                BaseConstants.CODE_VALIDATION_ERROR,
                BaseConstants.MSG_VALIDATION_ERROR,
                details
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        log.warn("Constraint violation: {}", details);
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                BaseConstants.CODE_CONSTRAINT_VIOLATION,
                BaseConstants.MSG_VALIDATION_ERROR,
                details
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ErrorResponseDto body = ErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(),
                BaseConstants.CODE_MALFORMED_JSON,
                BaseConstants.MSG_MALFORMED_JSON
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = ex.getName() + ": valor invalido";
        log.warn("Parameter type mismatch: {}", detail);
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                BaseConstants.CODE_INVALID_PARAMETER_TYPE,
                BaseConstants.MSG_VALIDATION_ERROR,
                List.of(detail)
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(MissingServletRequestParameterException ex) {
        String detail = ex.getParameterName() + ": parametro obligatorio";
        log.warn("Missing request parameter: {}", detail);
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                BaseConstants.CODE_MISSING_PARAMETER,
                BaseConstants.MSG_VALIDATION_ERROR,
                List.of(detail)
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Max upload size exceeded: {}", ex.getMessage());
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO,
                BaseConstants.MSG_MAX_UPLOAD_EXCEEDED,
                List.of(BaseConstants.MSG_MAX_UPLOAD_EXCEEDED)
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponseDto body = ErrorResponseDto.of(
                HttpStatus.NOT_FOUND.value(),
                BaseConstants.CODE_RESOURCE_NOT_FOUND,
                "Recurso no encontrado."
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        log.error("Unhandled exception in pockit-ms-auth-user", ex);
        ErrorResponseDto body = ErrorResponseDto.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                BaseConstants.CODE_INTERNAL_SERVER_ERROR,
                BaseConstants.MSG_INTERNAL_ERROR
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
