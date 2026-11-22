package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.RecuperarPasswordCommand;

public interface IRecuperarPasswordUseCase {

    void ejecutar(RecuperarPasswordCommand command);
}
