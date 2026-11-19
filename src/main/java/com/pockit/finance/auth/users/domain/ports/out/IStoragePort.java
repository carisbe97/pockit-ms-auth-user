package com.pockit.finance.auth.users.domain.ports.out;

import java.util.UUID;

public interface IStoragePort {

    String subirAvatar(UUID usrId, byte[] bytes, String contentType, String extension);
}
