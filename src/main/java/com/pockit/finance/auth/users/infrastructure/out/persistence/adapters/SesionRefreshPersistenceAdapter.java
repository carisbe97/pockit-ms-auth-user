package com.pockit.finance.auth.users.infrastructure.out.persistence.adapters;

import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.domain.ports.out.ISesionRefreshRepositoryPort;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.SesionRefreshEntity;
import com.pockit.finance.auth.users.infrastructure.out.persistence.mappers.ISesionRefreshPersistenceMapper;
import com.pockit.finance.auth.users.infrastructure.out.persistence.repositories.ISesionRefreshJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SesionRefreshPersistenceAdapter implements ISesionRefreshRepositoryPort {

    private final ISesionRefreshJpaRepository sesionRefreshJpaRepository;
    private final ISesionRefreshPersistenceMapper sesionRefreshPersistenceMapper;

    @Override
    public SesionRefresh save(SesionRefresh sesion) {
        SesionRefreshEntity entity = sesionRefreshPersistenceMapper.toEntity(sesion);
        SesionRefreshEntity saved = sesionRefreshJpaRepository.save(entity);
        return sesionRefreshPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<SesionRefresh> findByTokenHash(String tokenHash) {
        return sesionRefreshJpaRepository.findBySrfTokenHash(tokenHash)
                .map(sesionRefreshPersistenceMapper::toDomain);
    }

    @Override
    public void revocarTodasPorUsuario(UUID usrId) {
        sesionRefreshJpaRepository.revocarTodasPorUsuario(usrId);
    }
}
