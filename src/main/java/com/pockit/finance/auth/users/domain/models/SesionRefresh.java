package com.pockit.finance.auth.users.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo de dominio puro para rotacion y revocacion de Refresh Tokens (PK: Long / BIGSERIAL).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionRefresh {

    private Long srfId;
    private UUID srfUsrId;
    private String srfTokenHash;
    private OffsetDateTime srfExpiraEn;
    private Boolean srfRevocado;
    private String srfDispositivo;
    private OffsetDateTime srfCreadoEn;

    public boolean esValida(OffsetDateTime ahora) {
        return !Boolean.TRUE.equals(srfRevocado)
                && srfExpiraEn != null
                && ahora != null
                && srfExpiraEn.isAfter(ahora);
    }

    public void revocar() {
        this.srfRevocado = Boolean.TRUE;
    }
}
