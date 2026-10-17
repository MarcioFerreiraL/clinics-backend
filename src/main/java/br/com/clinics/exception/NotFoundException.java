package br.com.clinics.exception;

/** Recurso inexistente (mapeia para HTTP 404). */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
