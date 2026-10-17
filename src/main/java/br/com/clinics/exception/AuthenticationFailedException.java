package br.com.clinics.exception;

/** Credenciais inválidas (mapeia para HTTP 401). A mensagem é genérica de propósito (RN06). */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException() {
        super("INVALID_CREDENTIALS", "Telefone ou senha inválidos.");
    }
}
