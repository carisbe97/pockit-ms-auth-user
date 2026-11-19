package com.pockit.finance.auth.users.domain.ports.out;

import com.pockit.finance.auth.users.domain.models.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface IUsuarioRepositoryPort {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(UUID usrId);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
