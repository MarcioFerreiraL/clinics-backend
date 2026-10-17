package br.com.clinics.model;

import br.com.clinics.exception.ValidationException;

/** RN03: mínimo de 8 caracteres, com ao menos uma letra e um número. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        boolean valid = rawPassword != null
                && rawPassword.length() >= MIN_LENGTH
                && rawPassword.chars().anyMatch(Character::isLetter)
                && rawPassword.chars().anyMatch(Character::isDigit);
        if (!valid) {
            throw new ValidationException("WEAK_PASSWORD",
                    "A senha deve ter ao menos 8 caracteres, com letras e números.");
        }
    }
}
