package com.pockit.finance.auth.common.infrastructure.errors;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.exceptions.ImagenException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BaseControllerAdviceTest {

    private MockMvc mockMvc;
    private BaseControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new BaseControllerAdvice();
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(advice)
                .build();
    }

    @Test
    @DisplayName("Debe responder formato uniforme ante BaseDomainException e ImagenException")
    void shouldHandleDomainAndImagenException() throws Exception {
        mockMvc.perform(get("/test/domain-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.code").value("CORREO_YA_REGISTRADO"))
                .andExpect(jsonPath("$.message").value("Correo duplicado"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(get("/test/image-error"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.statusCode").value(413))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO));
    }

    @Test
    @DisplayName("Debe responder 400 con lista de campos ante MethodArgumentNotValidException")
    void shouldHandleValidationErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_VALIDATION_ERROR))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    @DisplayName("Debe responder 400 ante JSON mal formado, tipo invalido y parametro faltante")
    void shouldHandleBadRequestVariants() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json-roto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_MALFORMED_JSON));

        mockMvc.perform(get("/test/param").param("edad", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_INVALID_PARAMETER_TYPE));

        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_MISSING_PARAMETER));
    }

    @Test
    @DisplayName("Debe responder 500 generico sin exponer detalles internos ante Exception no controlada")
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.code").value(BaseConstants.CODE_INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.message").value(BaseConstants.MSG_INTERNAL_ERROR))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("Debe manejar ConstraintViolation, MaxUploadSizeExceeded y NoResourceFound")
    void shouldHandleDirectHandlers() {
        ResponseEntity<ErrorResponseDto> constraintRes = advice.handleConstraintViolation(
                new ConstraintViolationException("violacion", Set.of()));
        assertThat(constraintRes.getStatusCode().value()).isEqualTo(400);
        assertThat(constraintRes.getBody()).isNotNull();
        assertThat(constraintRes.getBody().code()).isEqualTo(BaseConstants.CODE_CONSTRAINT_VIOLATION);

        ResponseEntity<ErrorResponseDto> uploadRes = advice.handleMaxUploadSize(
                new MaxUploadSizeExceededException(BaseConstants.MAX_IMAGE_SIZE_BYTES));
        assertThat(uploadRes.getStatusCode().value()).isEqualTo(413);
        assertThat(uploadRes.getBody()).isNotNull();
        assertThat(uploadRes.getBody().code()).isEqualTo(BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO);

        ResponseEntity<ErrorResponseDto> notFoundRes = advice.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/no-existe"));
        assertThat(notFoundRes.getStatusCode().value()).isEqualTo(404);
        assertThat(notFoundRes.getBody()).isNotNull();
        assertThat(notFoundRes.getBody().code()).isEqualTo(BaseConstants.CODE_RESOURCE_NOT_FOUND);

        ErrorResponseDto nullDetailsDto = new ErrorResponseDto(400, "X", "msg", null);
        assertThat(nullDetailsDto.details()).isEmpty();

        ResponseEntity<ErrorResponseDto> missingPartRes = advice.handleMissingServletRequestPart(
                new MissingServletRequestPartException("datos"));
        assertThat(missingPartRes.getStatusCode().value()).isEqualTo(400);
        assertThat(missingPartRes.getBody()).isNotNull();
        assertThat(missingPartRes.getBody().code()).isEqualTo(BaseConstants.CODE_MISSING_PARAMETER);
        assertThat(missingPartRes.getBody().details()).containsExactly("datos: parte obligatoria");

        ResponseEntity<ErrorResponseDto> unsupportedMediaRes = advice.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException("Content-Type 'text/plain' is not supported"));
        assertThat(unsupportedMediaRes.getStatusCode().value()).isEqualTo(415);
        assertThat(unsupportedMediaRes.getBody()).isNotNull();
        assertThat(unsupportedMediaRes.getBody().code()).isEqualTo(BaseConstants.CODE_UNSUPPORTED_MEDIA_TYPE);
    }

    record DummyRequest(
            @NotBlank @Email String correo,
            @NotBlank String nombre
    ) {
    }

    @RestController
    static class DummyController {
        @GetMapping("/test/domain-error")
        public void domainError() {
            throw new BaseDomainException(409, "CORREO_YA_REGISTRADO", "Correo duplicado");
        }

        @GetMapping("/test/image-error")
        public void imageError() {
            throw new ImagenException(413, BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO, "Archivo muy grande");
        }

        @PostMapping("/test/validate")
        public void validate(@Valid @RequestBody DummyRequest request) {
        }

        @GetMapping("/test/param")
        public void param(@RequestParam Integer edad) {
        }

        @GetMapping("/test/unexpected")
        public void unexpected() {
            throw new IllegalStateException("Fallo interno sensible de BD");
        }
    }
}
