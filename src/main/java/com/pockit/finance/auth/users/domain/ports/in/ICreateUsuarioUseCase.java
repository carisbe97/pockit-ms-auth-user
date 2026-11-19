package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.CreateUsuarioCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;

public interface ICreateUsuarioUseCase {

    Usuario ejecutar(CreateUsuarioCommand command);
}
