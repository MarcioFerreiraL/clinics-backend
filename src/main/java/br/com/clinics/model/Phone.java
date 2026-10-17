package br.com.clinics.model;

import br.com.clinics.exception.ValidationException;

/**
 * Celular brasileiro normalizado: somente dígitos com DDI 55 (RN01, RN02).
 * Ex.: {@code (81) 99876-5432} vira {@code 5581998765432}.
 */
public record Phone(String digits) {

    private static final int LOCAL_LENGTH = 11;
    private static final int FULL_LENGTH = 13;

    public Phone {
        if (digits == null || !digits.matches("55[1-9][0-9]9[0-9]{8}")) {
            throw new ValidationException("INVALID_PHONE", "Informe um celular válido com DDD, por exemplo (81) 99876-5432.");
        }
    }

    public static Phone of(String raw) {
        if (raw == null) {
            throw new ValidationException("INVALID_PHONE", "Informe um celular válido com DDD, por exemplo (81) 99876-5432.");
        }
        String onlyDigits = raw.replaceAll("\\D", "");
        if (onlyDigits.length() == LOCAL_LENGTH) {
            onlyDigits = "55" + onlyDigits;
        }
        if (onlyDigits.length() != FULL_LENGTH) {
            throw new ValidationException("INVALID_PHONE", "Informe um celular válido com DDD, por exemplo (81) 99876-5432.");
        }
        return new Phone(onlyDigits);
    }

    /** Formato de exibição: {@code (81) 99876-5432}. */
    public String formatted() {
        return "(%s) %s-%s".formatted(digits.substring(2, 4), digits.substring(4, 9), digits.substring(9));
    }
}
