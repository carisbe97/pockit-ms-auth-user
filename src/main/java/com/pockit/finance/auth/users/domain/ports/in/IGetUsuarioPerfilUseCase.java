package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.Usuario;

import java.util.UUID;

public interface IGetUsuarioPerfilUseCase {

    Usuario ejecutar(UUID usrId);
}
