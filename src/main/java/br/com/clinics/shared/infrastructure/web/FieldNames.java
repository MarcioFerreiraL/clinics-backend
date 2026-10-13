package br.com.clinics.shared.infrastructure.web;

/** Converte nomes de propriedades Java para o {@code snake_case} usado no JSON da API. */
public final class FieldNames {

    private FieldNames() {
    }

    /** {@code fullName} vira {@code full_name}; caminhos aninhados ({@code user.fullName}) convertem cada segmento. */
    public static String toSnakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
