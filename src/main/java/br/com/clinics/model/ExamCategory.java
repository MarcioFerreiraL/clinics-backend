package br.com.clinics.model;

import java.text.Normalizer;
import java.util.Arrays;

import br.com.clinics.exception.ValidationException;

/** RN10: categorias válidas. O rótulo é o texto exibido ao paciente. */
public enum ExamCategory {
    SANGUE("Sangue"),
    RAIO_X("Raio-X"),
    CONSULTAS("Consultas"),
    CARDIO("Cardio"),
    ULTRASSOM("Ultrassom");

    private final String label;

    ExamCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Aceita o rótulo ({@code Raio-X}) ou o nome ({@code RAIO_X}), sem diferenciar caixa ou acentos. */
    public static ExamCategory parse(String value) {
        String wanted = normalize(value);
        return Arrays.stream(values())
                .filter(c -> normalize(c.name()).equals(wanted) || normalize(c.label).equals(wanted))
                .findFirst()
                .orElseThrow(() -> new ValidationException("INVALID_CATEGORY",
                        "Categoria inválida. Use: Sangue, Raio-X, Consultas, Cardio ou Ultrassom."));
    }

    private static String normalize(String text) {
        String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
