package br.com.clinics.controller;

import br.com.clinics.service.AuthenticateService;
import br.com.clinics.dto.AuthenticateCommand;
import br.com.clinics.service.RegisterUserService;
import br.com.clinics.dto.RegisterUserCommand;
import br.com.clinics.dto.AuthDtos.AuthResponse;
import br.com.clinics.dto.AuthDtos.LoginRequest;
import br.com.clinics.dto.AuthDtos.RegisterRequest;
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
public class AuthController {

    private final RegisterUserService register;
    private final AuthenticateService authenticate;

    public AuthController(RegisterUserService register, AuthenticateService authenticate) {
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
