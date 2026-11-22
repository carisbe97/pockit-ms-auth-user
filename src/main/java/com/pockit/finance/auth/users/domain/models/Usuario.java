package com.pockit.finance.auth.users.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo de dominio puro de Usuario (cero imports de Spring o JPA).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    private UUID usrId;
    private String usrCorreo;
    private String usrPasswordHash;
    private String usrNombre;
    private String usrApellido;
    private Short usrEdad;
    private String usrAvatarUrl;
    private Boolean usrActivo;
    private String usrResetTokenHash;
    private OffsetDateTime usrResetExpiraEn;
    private OffsetDateTime usrCreadoEn;
    private OffsetDateTime usrActualizadoEn;

    public boolean estaActivo() {
        return Boolean.TRUE.equals(usrActivo);
    }

    public boolean tokenRecuperacionValido(OffsetDateTime ahora) {
        return usrResetTokenHash != null
                && !usrResetTokenHash.isBlank()
                && usrResetExpiraEn != null
                && ahora != null
                && usrResetExpiraEn.isAfter(ahora);
    }

    public void actualizarAvatar(String nuevaUrl) {
        this.usrAvatarUrl = nuevaUrl;
    }

    public void asignarTokenRecuperacion(String tokenHash, OffsetDateTime expiraEn) {
        this.usrResetTokenHash = tokenHash;
        this.usrResetExpiraEn = expiraEn;
    }

    public void restablecerPassword(String nuevoPasswordHash) {
        this.usrPasswordHash = nuevoPasswordHash;
        this.usrResetTokenHash = null;
        this.usrResetExpiraEn = null;
    }
}
