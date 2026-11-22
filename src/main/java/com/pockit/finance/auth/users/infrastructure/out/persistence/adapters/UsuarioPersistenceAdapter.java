package com.pockit.finance.auth.users.infrastructure.out.persistence.adapters;

import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.domain.ports.out.IUsuarioRepositoryPort;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.UsuarioEntity;
import com.pockit.finance.auth.users.infrastructure.out.persistence.mappers.IUsuarioPersistenceMapper;
import com.pockit.finance.auth.users.infrastructure.out.persistence.repositories.IUsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements IUsuarioRepositoryPort {

    private final IUsuarioJpaRepository usuarioJpaRepository;
    private final IUsuarioPersistenceMapper usuarioPersistenceMapper;

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity entity = usuarioPersistenceMapper.toEntity(usuario);
        UsuarioEntity saved = usuarioJpaRepository.saveAndFlush(entity);
        return usuarioPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Usuario> findById(UUID usrId) {
        return usuarioJpaRepository.findById(usrId).map(usuarioPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByCorreo(String correo) {
        return usuarioJpaRepository.findByUsrCorreoIgnoreCase(correo).map(usuarioPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByResetTokenHash(String tokenHash) {
        return usuarioJpaRepository.findByUsrResetTokenHash(tokenHash).map(usuarioPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCorreo(String correo) {
        return usuarioJpaRepository.existsByUsrCorreoIgnoreCase(correo);
    }
}
