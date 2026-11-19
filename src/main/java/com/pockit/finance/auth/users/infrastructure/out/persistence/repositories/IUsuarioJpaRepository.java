package com.pockit.finance.auth.users.infrastructure.out.persistence.repositories;

import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByUsrCorreoIgnoreCase(String correo);

    boolean existsByUsrCorreoIgnoreCase(String correo);
}
