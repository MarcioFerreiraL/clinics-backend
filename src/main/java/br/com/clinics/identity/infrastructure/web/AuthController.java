package br.com.clinics.identity.infrastructure.web;

import br.com.clinics.identity.application.port.in.AuthenticateUseCase;
import br.com.clinics.identity.application.port.in.AuthenticateUseCase.AuthenticateCommand;
import br.com.clinics.identity.application.port.in.RegisterUserUseCase;
import br.com.clinics.identity.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import br.com.clinics.identity.infrastructure.web.AuthDtos.AuthResponse;
import br.com.clinics.identity.infrastructure.web.AuthDtos.LoginRequest;
import br.com.clinics.identity.infrastructure.web.AuthDtos.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
class AuthController {

    private final RegisterUserUseCase register;
    private final AuthenticateUseCase authenticate;

    AuthController(RegisterUserUseCase register, AuthenticateUseCase authenticate) {
        this.register = register;
        this.authenticate = authenticate;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um paciente (UC01)")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthResponse.from(register.register(
                new RegisterUserCommand(request.fullName(), request.phone(), request.cpf(), request.email(),
                        request.birthDate(), request.password())));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica com telefone e senha (UC02)")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthResponse.from(authenticate.authenticate(
                new AuthenticateCommand(request.phone(), request.password())));
    }
}
