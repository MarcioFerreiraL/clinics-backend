package br.com.clinics.model;

/** Privacidade (LGPD): avaliações públicas mostram só o primeiro nome e a inicial do último sobrenome. */
public final class ReviewerName {

    private ReviewerName() {
    }

    public static String abbreviate(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Paciente";
        }
        String[] parts = fullName.strip().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
