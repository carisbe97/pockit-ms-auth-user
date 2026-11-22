package com.pockit.finance.auth.users.infrastructure.config.in.mappers;

import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.Usuario;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.AuthTokensResponseDto;
import com.pockit.finance.auth.users.infrastructure.config.in.dto.UsuarioResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IUsuarioWebMapper {

    UsuarioResponseDto toResponseDto(Usuario usuario);

    AuthTokensResponseDto toAuthTokensResponseDto(AuthResult result);
}
