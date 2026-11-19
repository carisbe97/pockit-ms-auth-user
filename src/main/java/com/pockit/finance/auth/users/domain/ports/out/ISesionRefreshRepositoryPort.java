package com.pockit.finance.auth.users.domain.ports.out;

import com.pockit.finance.auth.users.domain.models.SesionRefresh;

import java.util.Optional;
import java.util.UUID;

public interface ISesionRefreshRepositoryPort {

    SesionRefresh save(SesionRefresh sesion);

    Optional<SesionRefresh> findByTokenHash(String tokenHash);

    void revocarTodasPorUsuario(UUID usrId);
}
