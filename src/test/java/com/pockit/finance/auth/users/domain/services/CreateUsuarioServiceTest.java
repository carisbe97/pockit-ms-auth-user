package com.pockit.finance.auth.users.domain.services;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.exceptions.ImagenException;
import com.pockit.finance.auth.common.infrastructure.utilities.BaseConstants;
import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.ICategoriaSistemaPort;
import com.pockit.finance.auth.users.domain.ports.out.IPasswordHasherPort;
import com.pockit.finance.auth.users.domain.ports.out.IStoragePort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUsuarioServiceTest {

    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private IPasswordHasherPort passwordHasherPort;
    @Mock
    private ICategoriaSistemaPort categoriaSistemaPort;
    @Mock
    private IStoragePort storagePort;

    private CreateUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new CreateUsuarioService(
                usuarioRepositoryPort,
                passwordHasherPort,
                categoriaSistemaPort,
                storagePort
        );
    }

    @Test
    @DisplayName("Debe registrar usuario sin avatar, normalizar correo, hashear password y sembrar categorias")
    void shouldRegisterUserWithoutAvatar() {
        UUID generatedId = UUID.randomUUID();
        CreateUsuarioCommand cmd = new CreateUsuarioCommand(
                "  Camilo.Perez@Pockit.COM ",
                "Pockit@2026",
                "  Camilo ",
                " Perez  ",
                (short) 28,
                null,
                null,
                null
        );

        when(usuarioRepositoryPort.existsByCorreo("camilo.perez@pockit.com")).thenReturn(false);
        when(passwordHasherPort.hash("Pockit@2026")).thenReturn("$2a$12$hashed");
        when(usuarioRepositoryPort.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setUsrId(generatedId);
            u.setUsrCreadoEn(OffsetDateTime.now());
            return u;
        });

        Usuario result = service.ejecutar(cmd);

        assertThat(result.getUsrId()).isEqualTo(generatedId);
        assertThat(result.getUsrCorreo()).isEqualTo("camilo.perez@pockit.com");
        assertThat(result.getUsrPasswordHash()).isEqualTo("$2a$12$hashed");
        assertThat(result.getUsrNombre()).isEqualTo("Camilo");
        assertThat(result.getUsrApellido()).isEqualTo("Perez");
        assertThat(result.getUsrEdad()).isEqualTo((short) 28);
        assertThat(result.getUsrActivo()).isTrue();
        assertThat(result.getUsrAvatarUrl()).isNull();

        verify(usuarioRepositoryPort, times(1)).save(any(Usuario.class));
        verify(categoriaSistemaPort).crearCategoriasSistema(generatedId);
        verify(storagePort, never()).subirAvatar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe registrar usuario con avatar, subir a storage y actualizar usrAvatarUrl")
    void shouldRegisterUserWithAvatar() {
        UUID generatedId = UUID.randomUUID();
        byte[] avatar = new byte[]{1, 2, 3, 4};
        CreateUsuarioCommand cmd = new CreateUsuarioCommand(
                "ana@pockit.com",
                "Pockit@2026",
                "Ana",
                "Gomez",
                (short) 25,
                avatar,
                "image/png",
                "png"
        );

        when(usuarioRepositoryPort.existsByCorreo("ana@pockit.com")).thenReturn(false);
        when(passwordHasherPort.hash("Pockit@2026")).thenReturn("$2a$12$hashed");
        when(usuarioRepositoryPort.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            if (u.getUsrId() == null) {
                u.setUsrId(generatedId);
            }
            return u;
        });
        when(storagePort.subirAvatar(generatedId, avatar, "image/png", "png"))
                .thenReturn("https://supabase.test/storage/v1/object/public/avatars/" + generatedId + "/avatar.png");

        Usuario result = service.ejecutar(cmd);

        assertThat(result.getUsrAvatarUrl())
                .isEqualTo("https://supabase.test/storage/v1/object/public/avatars/" + generatedId + "/avatar.png");
        verify(categoriaSistemaPort).crearCategoriasSistema(generatedId);
        verify(storagePort).subirAvatar(generatedId, avatar, "image/png", "png");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositoryPort, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getUsrAvatarUrl()).contains("avatar.png");
    }

    @Test
    @DisplayName("Debe lanzar 409 CORREO_YA_REGISTRADO cuando el correo ya existe")
    void shouldThrow409WhenEmailAlreadyExists() {
        CreateUsuarioCommand cmd = new CreateUsuarioCommand(
                "duplicado@pockit.com", "Pockit@2026", "Juan", "Dia", (short) 30, null, null, null
        );
        when(usuarioRepositoryPort.existsByCorreo("duplicado@pockit.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ejecutar(cmd))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(409);
                    assertThat(bde.getCode()).isEqualTo(BaseConstants.CODE_CORREO_YA_REGISTRADO);
                });

        verify(usuarioRepositoryPort, never()).save(any());
        verify(categoriaSistemaPort, never()).crearCategoriasSistema(any());
    }

    @Test
    @DisplayName("Debe rechazar comando nulo, correo invalido, password debil, edad invalida y nombre/apellido vacios")
    void shouldValidateDomainRules() {
        assertThatThrownBy(() -> service.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getStatusCode()).isEqualTo(400));

        CreateUsuarioCommand badEmail = new CreateUsuarioCommand(
                "correo-invalido", "Pockit@2026", "A", "B", (short) 20, null, null, null
        );
        assertThatThrownBy(() -> service.ejecutar(badEmail))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo(BaseConstants.CODE_CORREO_INVALIDO));

        CreateUsuarioCommand weakPassword = new CreateUsuarioCommand(
                "ok@pockit.com", "1234", "A", "B", (short) 20, null, null, null
        );
        assertThatThrownBy(() -> service.ejecutar(weakPassword))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo(BaseConstants.CODE_PASSWORD_DEBIL));

        CreateUsuarioCommand negativeAge = new CreateUsuarioCommand(
                "ok@pockit.com", "Pockit@2026", "A", "B", (short) -1, null, null, null
        );
        assertThatThrownBy(() -> service.ejecutar(negativeAge))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo(BaseConstants.CODE_EDAD_INVALIDA));

        CreateUsuarioCommand overAge = new CreateUsuarioCommand(
                "ok@pockit.com", "Pockit@2026", "A", "B", (short) 131, null, null, null
        );
        assertThatThrownBy(() -> service.ejecutar(overAge))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo(BaseConstants.CODE_EDAD_INVALIDA));

        CreateUsuarioCommand blankName = new CreateUsuarioCommand(
                "ok@pockit.com", "Pockit@2026", "   ", "B", (short) 20, null, null, null
        );
        assertThatThrownBy(() -> service.ejecutar(blankName))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> assertThat(((BaseDomainException) ex).getCode()).isEqualTo(BaseConstants.CODE_VALIDATION_ERROR));
    }

    @Test
    @DisplayName("Debe propagar excepcion cuando falla la subida de avatar a storage")
    void shouldPropagateStorageFailure() {
        UUID generatedId = UUID.randomUUID();
        byte[] avatar = new byte[]{1, 2, 3};
        CreateUsuarioCommand cmd = new CreateUsuarioCommand(
                "fail@pockit.com", "Pockit@2026", "Luis", "Mora", (short) 33, avatar, "image/png", "png"
        );

        when(usuarioRepositoryPort.existsByCorreo("fail@pockit.com")).thenReturn(false);
        when(passwordHasherPort.hash("Pockit@2026")).thenReturn("$2a$12$hashed");
        when(usuarioRepositoryPort.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setUsrId(generatedId);
            return u;
        });
        when(storagePort.subirAvatar(generatedId, avatar, "image/png", "png"))
                .thenThrow(new ImagenException(502, BaseConstants.CODE_IMAGEN_SUBIDA_ERROR, "Error de Supabase"));

        assertThatThrownBy(() -> service.ejecutar(cmd))
                .isInstanceOf(ImagenException.class)
                .satisfies(ex -> {
                    ImagenException ie = (ImagenException) ex;
                    assertThat(ie.getStatusCode()).isEqualTo(502);
                    assertThat(ie.getCode()).isEqualTo(BaseConstants.CODE_IMAGEN_SUBIDA_ERROR);
                });

        verify(usuarioRepositoryPort, times(1)).save(any(Usuario.class));
    }
}
