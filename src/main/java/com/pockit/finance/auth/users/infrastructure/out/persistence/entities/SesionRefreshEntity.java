package com.pockit.finance.auth.users.infrastructure.out.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "sesiones_refresh", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionRefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "srf_id", nullable = false, updatable = false)
    private Long srfId;

    @Column(name = "srf_usr_id", nullable = false)
    private UUID srfUsrId;

    @Column(name = "srf_token_hash", nullable = false, unique = true, length = 255)
    private String srfTokenHash;

    @Column(name = "srf_expira_en", nullable = false)
    private OffsetDateTime srfExpiraEn;

    @Column(name = "srf_revocado", nullable = false)
    private Boolean srfRevocado;

    @Column(name = "srf_dispositivo", length = 120)
    private String srfDispositivo;

    @Column(name = "srf_creado_en", nullable = false, updatable = false)
    private OffsetDateTime srfCreadoEn;

    @PrePersist
    void prePersist() {
        if (this.srfRevocado == null) {
            this.srfRevocado = Boolean.FALSE;
        }
        if (this.srfCreadoEn == null) {
            this.srfCreadoEn = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
