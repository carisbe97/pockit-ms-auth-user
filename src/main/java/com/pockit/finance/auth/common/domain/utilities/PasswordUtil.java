package com.pockit.finance.auth.common.domain.utilities;

/**
 * Utilidad de dominio puro para validacion de fortaleza de contrasenas.
 */
public final class PasswordUtil {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private PasswordUtil() {
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isWhitespace(ch)) {
                return false;
            }
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
