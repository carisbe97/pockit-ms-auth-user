package com.pockit.finance.auth.users.infrastructure.out.persistence.mappers;

import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.UsuarioEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IUsuarioPersistenceMapper {

    Usuario toDomain(UsuarioEntity entity);

    UsuarioEntity toEntity(Usuario domain);
}
