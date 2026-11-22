package com.pockit.finance.auth.users.application.usecases;

import com.pockit.finance.auth.common.domain.exceptions.BaseDomainException;
import com.pockit.finance.auth.common.domain.utilities.TokenHashUtil;
import com.pockit.finance.auth.users.domain.models.RecuperarPasswordCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.IRecoveryNotificationPort;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperarPasswordServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-22T18:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private IUsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private IRecoveryNotificationPort recoveryNotificationPort;

    private RecuperarPasswordService service;

    @BeforeEach
    void setUp() {
        service = new RecuperarPasswordService(usuarioRepositoryPort, recoveryNotificationPort, 15L, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Debe generar token opaco, guardar hash SHA-256 con expiracion y notificar al usuario activo")
    void shouldAssignResetTokenAndNotifyActiveUser() {
        Usuario usuario = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrCorreo("camilo@pockit.com")
                .usrActivo(true)
                .build();

        when(usuarioRepositoryPort.findByCorreo("camilo@pockit.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepositoryPort.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar(new RecuperarPasswordCommand("  CAMILO@Pockit.com  "));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> expiraCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(recoveryNotificationPort).enviarTokenRecuperacion(
                org.mockito.ArgumentMatchers.eq("camilo@pockit.com"),
                tokenCaptor.capture(),
                expiraCaptor.capture()
        );

        String rawToken = tokenCaptor.getValue();
        OffsetDateTime expectedExpira = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusMinutes(15);
        assertThat(rawToken).isNotBlank();
        assertThat(expiraCaptor.getValue()).isEqualTo(expectedExpira);
        assertThat(usuario.getUsrResetTokenHash()).isEqualTo(TokenHashUtil.sha256Hex(rawToken));
        assertThat(usuario.getUsrResetExpiraEn()).isEqualTo(expectedExpira);
        verify(usuarioRepositoryPort).save(usuario);
    }

    @Test
    @DisplayName("Debe completar silenciosamente sin error cuando el correo no existe (anti-enumeracion)")
    void shouldCompleteSilentlyWhenUserNotFound() {
        when(usuarioRepositoryPort.findByCorreo("noexiste@pockit.com")).thenReturn(Optional.empty());

        service.ejecutar(new RecuperarPasswordCommand("noexiste@pockit.com"));

        verify(usuarioRepositoryPort, never()).save(any());
        verifyNoInteractions(recoveryNotificationPort);
    }

    @Test
    @DisplayName("Debe completar silenciosamente cuando el usuario esta inactivo")
    void shouldCompleteSilentlyWhenUserIsInactive() {
        Usuario inactivo = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrCorreo("inactivo@pockit.com")
                .usrActivo(false)
                .build();
        when(usuarioRepositoryPort.findByCorreo("inactivo@pockit.com")).thenReturn(Optional.of(inactivo));

        service.ejecutar(new RecuperarPasswordCommand("inactivo@pockit.com"));

        verify(usuarioRepositoryPort, never()).save(any());
        verifyNoInteractions(recoveryNotificationPort);
    }

    @Test
    @DisplayName("Debe lanzar 400 VALIDATION_ERROR cuando el comando o correo es nulo o blanco")
    void shouldThrow400WhenCommandOrCorreoInvalid() {
        assertThatThrownBy(() -> service.ejecutar(null))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(400);
                    assertThat(bde.getCode()).isEqualTo("VALIDATION_ERROR");
                });

        assertThatThrownBy(() -> service.ejecutar(new RecuperarPasswordCommand("   ")))
                .isInstanceOf(BaseDomainException.class)
                .satisfies(ex -> {
                    BaseDomainException bde = (BaseDomainException) ex;
                    assertThat(bde.getStatusCode()).isEqualTo(400);
                    assertThat(bde.getCode()).isEqualTo("VALIDATION_ERROR");
                });
    }

    @Test
    @DisplayName("Constructor sin Clock debe usar Clock.systemUTC por defecto")
    void shouldSupportDefaultUtcClockConstructor() {
        RecuperarPasswordService defaultClockService = new RecuperarPasswordService(
                usuarioRepositoryPort, recoveryNotificationPort, 15L
        );
        when(usuarioRepositoryPort.findByCorreo("ghost@pockit.com")).thenReturn(Optional.empty());
        defaultClockService.ejecutar(new RecuperarPasswordCommand("ghost@pockit.com"));
        verifyNoInteractions(recoveryNotificationPort);
    }
}
