package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.RestablecerPasswordCommand;

public interface IRestablecerPasswordUseCase {

    void ejecutar(RestablecerPasswordCommand command);
}
