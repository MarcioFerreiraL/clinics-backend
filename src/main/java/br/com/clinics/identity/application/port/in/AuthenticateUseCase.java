package br.com.clinics.identity.application.port.in;

import br.com.clinics.identity.application.AuthResult;

/** UC02 · Autenticar com telefone e senha. */
public interface AuthenticateUseCase {

    AuthResult authenticate(AuthenticateCommand command);

    record AuthenticateCommand(String phone, String password) {
    }
}
