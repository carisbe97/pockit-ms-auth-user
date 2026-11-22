package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.LoginCommand;

public interface ILoginUseCase {

    AuthResult ejecutar(LoginCommand command);
}
