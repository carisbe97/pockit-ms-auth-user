package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.AuthResult;
import com.pockit.finance.auth.users.domain.models.RefreshTokenCommand;

public interface IRefreshTokenUseCase {

    AuthResult ejecutar(RefreshTokenCommand command);
}
