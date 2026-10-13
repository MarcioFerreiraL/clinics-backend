package br.com.clinics.identity.application.port.in;

import br.com.clinics.identity.application.AuthResult;

/** UC01 · Cadastrar conta de paciente. */
public interface RegisterUserUseCase {

    AuthResult register(RegisterUserCommand command);

    record RegisterUserCommand(String fullName, String phone, String password) {
    }
}
