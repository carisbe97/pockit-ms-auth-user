package com.pockit.finance.auth.users.domain.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioAndSesionRefreshTest {

    @Test
    @DisplayName("Usuario debe evaluar estado activo, validez de token de recuperacion y actualizacion de avatar")
    void shouldEvaluateUsuarioDomainRules() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        Usuario usuario = Usuario.builder()
                .usrId(UUID.randomUUID())
                .usrCorreo("camilo@pockit.com")
                .usrActivo(true)
                .usrResetTokenHash("hash-sha256")
                .usrResetExpiraEn(ahora.plusMinutes(15))
                .build();

        assertThat(usuario.estaActivo()).isTrue();
        assertThat(usuario.tokenRecuperacionValido(ahora)).isTrue();
        assertThat(usuario.tokenRecuperacionValido(ahora.plusMinutes(16))).isFalse();
        assertThat(usuario.tokenRecuperacionValido(null)).isFalse();

        usuario.setUsrResetTokenHash("   ");
        assertThat(usuario.tokenRecuperacionValido(ahora)).isFalse();

        usuario.setUsrResetTokenHash(null);
        assertThat(usuario.tokenRecuperacionValido(ahora)).isFalse();

        usuario.setUsrResetTokenHash("hash");
        usuario.setUsrResetExpiraEn(null);
        assertThat(usuario.tokenRecuperacionValido(ahora)).isFalse();

        usuario.actualizarAvatar("https://supabase.co/storage/v1/object/public/pockit-avatars/avatar.png");
        assertThat(usuario.getUsrAvatarUrl()).contains("pockit-avatars");

        usuario.setUsrActivo(false);
        assertThat(usuario.estaActivo()).isFalse();

        OffsetDateTime expira = ahora.plusMinutes(15);
        usuario.asignarTokenRecuperacion("nuevo-reset-hash", expira);
        assertThat(usuario.getUsrResetTokenHash()).isEqualTo("nuevo-reset-hash");
        assertThat(usuario.getUsrResetExpiraEn()).isEqualTo(expira);
        assertThat(usuario.tokenRecuperacionValido(ahora)).isTrue();

        usuario.restablecerPassword("$2a$12$nuevoPasswordHash");
        assertThat(usuario.getUsrPasswordHash()).isEqualTo("$2a$12$nuevoPasswordHash");
        assertThat(usuario.getUsrResetTokenHash()).isNull();
        assertThat(usuario.getUsrResetExpiraEn()).isNull();
        assertThat(usuario.tokenRecuperacionValido(ahora)).isFalse();
    }

    @Test
    @DisplayName("SesionRefresh debe validar expiracion y revocacion correctamente")
    void shouldEvaluateSesionRefreshRules() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        SesionRefresh sesion = SesionRefresh.builder()
                .srfId(1L)
                .srfUsrId(UUID.randomUUID())
                .srfTokenHash("token-hash")
                .srfExpiraEn(ahora.plusDays(7))
                .srfRevocado(false)
                .srfDispositivo("Android Pixel")
                .build();

        assertThat(sesion.esValida(ahora)).isTrue();
        assertThat(sesion.esValida(ahora.plusDays(8))).isFalse();
        assertThat(sesion.esValida(null)).isFalse();

        sesion.revocar();
        assertThat(sesion.getSrfRevocado()).isTrue();
        assertThat(sesion.esValida(ahora)).isFalse();

        SesionRefresh sinFecha = new SesionRefresh();
        assertThat(sinFecha.esValida(ahora)).isFalse();
    }
}
