package com.pockit.finance.auth.users.domain.ports.in;

import com.pockit.finance.auth.users.domain.models.UpdateAvatarCommand;
import com.pockit.finance.auth.users.domain.models.Usuario;

public interface IUpdateAvatarUseCase {

    Usuario ejecutar(UpdateAvatarCommand command);
}
