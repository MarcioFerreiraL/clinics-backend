package br.com.clinics.exception;

/** Operação conflita com o estado atual, por exemplo recurso duplicado (mapeia para HTTP 409). */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
