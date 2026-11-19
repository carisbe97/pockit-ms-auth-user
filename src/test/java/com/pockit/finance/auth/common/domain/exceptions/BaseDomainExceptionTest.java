package com.pockit.finance.auth.common.domain.exceptions;

import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseDomainExceptionTest {

    @Test
    @DisplayName("Debe construir BaseDomainException con detalles inmutables y valores por defecto")
    void shouldConstructBaseDomainException() {
        BaseDomainException exSimple = new BaseDomainException(409, "CORREO_YA_REGISTRADO", "El correo ya existe");
        assertThat(exSimple.getStatusCode()).isEqualTo(409);
        assertThat(exSimple.getCode()).isEqualTo("CORREO_YA_REGISTRADO");
        assertThat(exSimple.getMessage()).isEqualTo("El correo ya existe");
        assertThat(exSimple.getDetails()).isEmpty();

        BaseDomainException exWithDetails = new BaseDomainException(422, "ERROR_LOTE", "Fallo en lote", List.of("item 1"));
        assertThat(exWithDetails.getDetails()).containsExactly("item 1");

        BaseDomainException exNullDetails = new BaseDomainException(400, "BAD", "Bad", (List<String>) null);
        assertThat(exNullDetails.getDetails()).isEmpty();

        RuntimeException cause = new RuntimeException("root");
        BaseDomainException exWithCause = new BaseDomainException(500, "ERR", "Fallo", cause);
        assertThat(exWithCause.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("Debe construir ImagenException con codigos HTTP 400 y 413")
    void shouldConstructImagenException() {
        ImagenException exDefault = new ImagenException(BaseConstants.CODE_IMAGEN_CORRUPTA, "Imagen corrupta");
        assertThat(exDefault.getStatusCode()).isEqualTo(400);
        assertThat(exDefault.getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_CORRUPTA);

        ImagenException exTooLarge = new ImagenException(413, BaseConstants.CODE_IMAGEN_TAMANO_EXCEDIDO, "Excede 2MB");
        assertThat(exTooLarge.getStatusCode()).isEqualTo(413);

        IllegalArgumentException cause = new IllegalArgumentException("bad bytes");
        ImagenException exWithCause = new ImagenException(BaseConstants.CODE_IMAGEN_CORRUPTA, "No legible", cause);
        assertThat(exWithCause.getStatusCode()).isEqualTo(400);
        assertThat(exWithCause.getCause()).isSameAs(cause);
    }
}
