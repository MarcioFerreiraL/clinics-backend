package br.com.clinics.shared.domain;

/** Violação de regra de negócio. O código estável permite ao cliente tratar o erro sem ler a mensagem. */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
