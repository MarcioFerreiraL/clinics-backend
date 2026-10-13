package br.com.clinics.identity.domain;

import br.com.clinics.shared.domain.ValidationException;

/** CPF normalizado em 11 dígitos, com validação dos dígitos verificadores (RN37). */
public record Cpf(String digits) {

    public Cpf {
        if (digits == null || !isValid(digits)) {
            throw new ValidationException("INVALID_CPF", "Informe um CPF válido.");
        }
    }

    public static Cpf of(String raw) {
        return new Cpf(raw == null ? null : raw.replaceAll("\\D", ""));
    }

    private static boolean isValid(String d) {
        if (!d.matches("\\d{11}") || d.chars().distinct().count() == 1) {
            return false;
        }
        return check(d, 9) == d.charAt(9) - '0' && check(d, 10) == d.charAt(10) - '0';
    }

    private static int check(String d, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (d.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    /** Formato de exibição: {@code 123.456.789-09}. */
    public String formatted() {
        return "%s.%s.%s-%s".formatted(digits.substring(0, 3), digits.substring(3, 6), digits.substring(6, 9),
                digits.substring(9));
    }
}
