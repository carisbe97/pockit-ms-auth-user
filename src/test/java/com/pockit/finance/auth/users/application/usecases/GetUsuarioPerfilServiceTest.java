package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUsuarioPerfilServiceTest {

    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;

    private GetUsuarioPerfilService getUsuarioPerfilService;

    @BeforeEach
    void setUp() {
        getUsuarioPerfilService = new GetUsuarioPerfilService(usuarioRepositoryPort);
    }

    @Test
    @DisplayName("Debe retornar el perfil del usuario cuando existe")
    void shouldReturnUserProfileWhenFound() {
        UUID usrId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrNombre("Camilo")
                .build();

        when(usuarioRepositoryPort.findById(usrId)).thenReturn(Optional.of(usuario));

        Usuario result = getUsuarioPerfilService.ejecutar(usrId);

        assertThat(result).isSameAs(usuario);
    }

    @Test
    @DisplayName("Debe lanzar 404 USUARIO_NO_ENCONTRADO cuando el usuario no existe")
    void shouldThrow404WhenUserNotFound() {
        UUID usrId = UUID.randomUUID();
        when(usuarioRepositoryPort.findById(usrId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getUsuarioPerfilService.ejecutar(usrId))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(404);
                    assertThat(domainEx.getCode()).isEqualTo("USUARIO_NO_ENCONTRADO");
                });
    }

    @Test
    @DisplayName("Debe lanzar 401 NO_AUTENTICADO cuando usrId es nulo")
    void shouldThrow401WhenUsrIdIsNull() {
        assertThatThrownBy(() -> getUsuarioPerfilService.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException domainEx = (BaseDomainException) ex;
                    assertThat(domainEx.getStatusCode()).isEqualTo(401);
                    assertThat(domainEx.getCode()).isEqualTo("NO_AUTENTICADO");
                });
    }
}
