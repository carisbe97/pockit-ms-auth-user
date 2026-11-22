package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.users.domain.models.UpdateAvatarCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAvatarServiceTest {

    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private IStoragePort storagePort;

    private UpdateAvatarService service;

    @BeforeEach
    void setUp() {
        service = new UpdateAvatarService(usuarioRepositoryPort, storagePort);
    }

    @Test
    @DisplayName("Debe subir nuevo avatar a storage y actualizar usrAvatarUrl del usuario")
    void shouldUploadAvatarAndUpdateUsuario() {
        UUID usrId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3, 4};
        Usuario usuario = Usuario.builder()
                .usrId(usrId)
                .usrCorreo("camilo@pockit.com")
                .usrActivo(true)
                .build();
        String expectedUrl = "https://supabase.test/storage/v1/object/public/avatars/" + usrId + "/avatar.png";

        when(usuarioRepositoryPort.findById(usrId)).thenReturn(Optional.of(usuario));
        when(storagePort.subirAvatar(usrId, bytes, "image/png", "png")).thenReturn(expectedUrl);
        when(usuarioRepositoryPort.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario result = service.ejecutar(new UpdateAvatarCommand(usrId, bytes, "image/png", "png"));

        assertThat(result.getUsrAvatarUrl()).isEqualTo(expectedUrl);
        verify(storagePort).subirAvatar(usrId, bytes, "image/png", "png");
        verify(usuarioRepositoryPort).save(usuario);
    }

    @Test
    @DisplayName("Debe lanzar 404 USUARIO_NO_ENCONTRADO cuando el usuario no existe")
    void shouldThrow404WhenUserNotFound() {
        UUID usrId = UUID.randomUUID();
        when(usuarioRepositoryPort.findById(usrId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(new UpdateAvatarCommand(usrId, new byte[]{1}, "image/png", "png")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(404);
                    assertThat(bde.getCode()).isEqualTo("USUARIO_NO_ENCONTRADO");
                });
        verifyNoInteractions(storagePort);
    }

    @Test
    @DisplayName("Debe validar comando nulo, usrId nulo y bytes vacios")
    void shouldValidateCommandFields() {
        assertThatThrownBy(() -> service.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getStatusCode()).isEqualTo(400));

        assertThatThrownBy(() -> service.ejecutar(new UpdateAvatarCommand(null, new byte[]{1}, "image/png", "png")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getStatusCode()).isEqualTo(401));

        assertThatThrownBy(() -> service.ejecutar(new UpdateAvatarCommand(UUID.randomUUID(), new byte[0], "image/png", "png")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(400);
                    assertThat(bde.getCode()).isEqualTo("IMAGEN_VACIA");
                });
    }
}
