package com.pockit.finance.auth.users.infrastructure.out.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "usuarios", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usr_id", nullable = false, updatable = false)
    private UUID usrId;

    @Column(name = "usr_correo", nullable = false, unique = true, length = 255)
    private String usrCorreo;

    @Column(name = "usr_password_hash", nullable = false, length = 255)
    private String usrPasswordHash;

    @Column(name = "usr_nombre", nullable = false, length = 100)
    private String usrNombre;

    @Column(name = "usr_apellido", nullable = false, length = 100)
    private String usrApellido;

    @Column(name = "usr_edad", nullable = false)
    private Short usrEdad;

    @Column(name = "usr_avatar_url", columnDefinition = "TEXT")
    private String usrAvatarUrl;

    @Column(name = "usr_activo", nullable = false)
    private Boolean usrActivo;

    @Column(name = "usr_reset_token_hash", length = 255)
    private String usrResetTokenHash;

    @Column(name = "usr_reset_expira_en")
    private OffsetDateTime usrResetExpiraEn;

    @Column(name = "usr_creado_en", nullable = false, updatable = false)
    private OffsetDateTime usrCreadoEn;

    @Column(name = "usr_actualizado_en", nullable = false)
    private OffsetDateTime usrActualizadoEn;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.usrActivo == null) {
            this.usrActivo = Boolean.TRUE;
        }
        if (this.usrCreadoEn == null) {
            this.usrCreadoEn = now;
        }
        this.usrActualizadoEn = now;
    }

    @PreUpdate
    void preUpdate() {
        this.usrActualizadoEn = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
