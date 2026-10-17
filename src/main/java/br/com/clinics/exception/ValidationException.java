package br.com.clinics.exception;

/** Dado de entrada que viola uma regra do domínio (mapeia para HTTP 400). */
public class ValidationException extends DomainException {

    public ValidationException(String code, String message) {
        super(code, message);
    }
}
