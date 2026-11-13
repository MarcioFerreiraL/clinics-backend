package br.com.clinics.user;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static br.com.clinics.common.SecurityConfig.userId;

@RestController
@Tag(name = "Usuário")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um paciente")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = users.register(request.fullName(), request.phone(), request.cpf(), request.email(),
                request.birthDate(), request.password());
        return authResponse(user);
    }

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "Entra com telefone e senha")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authResponse(users.login(request.phone(), request.password()));
    }

    @GetMapping("/api/v1/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Consulta o próprio perfil")
    ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(users.find(userId(jwt)));
    }

    @PutMapping("/api/v1/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Edita nome, e-mail e data de nascimento")
    ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return ProfileResponse.from(users.updateProfile(userId(jwt), request.fullName(), request.email(),
                request.birthDate()));
    }

    @PutMapping(path = "/api/v1/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Troca a foto de perfil (JPEG ou PNG de até 2 MB)")
    ProfileResponse avatar(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) throws IOException {
        return ProfileResponse.from(users.changeAvatar(userId(jwt), file.getBytes()));
    }

    @PutMapping("/api/v1/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Altera a senha")
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(userId(jwt), request.currentPassword(), request.newPassword());
    }

    private AuthResponse authResponse(User user) {
        UserService.Token token = users.issueToken(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(),
                new UserSummary(user.id(), user.fullName(), UserService.formatPhone(user.phone()), user.role()));
    }

    record RegisterRequest(
            @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "Nome muito longo.") String fullName,
            @NotBlank(message = "Informe seu celular.") String phone,
            @NotBlank(message = "Informe seu CPF.") String cpf,
            @NotBlank(message = "Informe seu e-mail.") @Size(max = 255, message = "E-mail muito longo.") String email,
            @NotNull(message = "Informe sua data de nascimento.") LocalDate birthDate,
            @NotBlank(message = "Informe uma senha.") String password) {
    }

    record LoginRequest(
            @NotBlank(message = "Informe seu celular.") String phone,
            @NotBlank(message = "Informe sua senha.") String password) {
    }

    /** O telefone fica de fora de propósito: não é editável pelo perfil. */
    record UpdateProfileRequest(
            @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "Nome muito longo.") String fullName,
            @Size(max = 255, message = "E-mail muito longo.") String email,
            LocalDate birthDate) {
    }

    record ChangePasswordRequest(
            @NotBlank(message = "Informe a senha atual.") String currentPassword,
            @NotBlank(message = "Informe a nova senha.") String newPassword) {
    }

    record UserSummary(UUID id, String fullName, String phone, String role) {
    }

    record AuthResponse(String token, String tokenType, Instant expiresAt, UserSummary user) {
    }

    record ProfileResponse(UUID id, String fullName, String phone, String cpf, String email, LocalDate birthDate,
            String avatarUrl, String role, Instant createdAt) {

        static ProfileResponse from(User u) {
            return new ProfileResponse(u.id(), u.fullName(), UserService.formatPhone(u.phone()),
                    UserService.formatCpf(u.cpf()), u.email(), u.birthDate(), u.avatarUrl(), u.role(), u.createdAt());
        }
    }
}
