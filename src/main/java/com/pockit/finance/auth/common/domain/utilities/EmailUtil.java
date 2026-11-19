package com.pockit.finance.auth.common.domain.utilities;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilidad de dominio puro para normalizacion y validacion de correos electronicos.
 */
public final class EmailUtil {

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9._+-]*[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$"
    );

    private EmailUtil() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        String normalized = normalize(email);
        if (normalized == null || normalized.isEmpty() || normalized.length() > MAX_EMAIL_LENGTH) {
            return false;
        }
        if (normalized.contains("..")) {
            return false;
        }
        return EMAIL_PATTERN.matcher(normalized).matches();
    }
}
