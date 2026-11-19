package com.pockit.finance.auth.users.domain.ports.out;

public interface IPasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
