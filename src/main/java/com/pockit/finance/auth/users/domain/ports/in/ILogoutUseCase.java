package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.LogoutCommand;

public interface ILogoutUseCase {

    void ejecutar(LogoutCommand command);
}
