package br.com.clinics.controller;

import java.io.IOException;
import java.util.UUID;

import br.com.clinics.service.ChangePasswordService;
import br.com.clinics.dto.ChangePasswordCommand;
import br.com.clinics.service.ManageProfileService;
import br.com.clinics.dto.UpdateProfileCommand;
import br.com.clinics.dto.ProfileDtos.ChangePasswordRequest;
import br.com.clinics.dto.ProfileDtos.ProfileResponse;
import br.com.clinics.dto.ProfileDtos.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Todas as operações valem para o usuário do token: o id nunca vem do corpo nem da URL (RN35). */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Perfil")
@SecurityRequirement(name = "bearer-jwt")
public class MeController {

    private final ManageProfileService profile;
    private final ChangePasswordService password;

    public MeController(ManageProfileService profile, ChangePasswordService password) {
        this.profile = profile;
        this.password = password;
    }

    @GetMapping
    @Operation(summary = "Consulta o próprio perfil (UC11)")
    ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profile.getProfile(userId(jwt)));
    }

    @PutMapping
    @Operation(summary = "Edita nome, e-mail e data de nascimento (UC11)")
    ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return ProfileResponse.from(profile.updateProfile(
                new UpdateProfileCommand(userId(jwt), request.fullName(), request.email(), request.birthDate())));
    }

    @PutMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualiza a foto de perfil, JPEG ou PNG de até 2 MB (UC11)")
    ProfileResponse avatar(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) throws IOException {
        return ProfileResponse.from(profile.changeAvatar(userId(jwt), file.getBytes()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Altera a senha (UC12)")
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        password.changePassword(new ChangePasswordCommand(userId(jwt), request.currentPassword(), request.newPassword()));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
