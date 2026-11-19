package com.pockit.finance.auth.users.infrastructure.out.persistence.repositories;

import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.SesionRefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ISesionRefreshJpaRepository extends JpaRepository<SesionRefreshEntity, Long> {

    Optional<SesionRefreshEntity> findBySrfTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE SesionRefreshEntity s SET s.srfRevocado = true WHERE s.srfUsrId = :usrId AND s.srfRevocado = false")
    void revocarTodasPorUsuario(@Param("usrId") UUID usrId);
}
