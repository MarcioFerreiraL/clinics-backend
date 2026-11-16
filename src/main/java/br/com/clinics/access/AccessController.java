package br.com.clinics.access;

import java.util.List;
import java.util.UUID;

import br.com.clinics.access.AccessService.Matrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static br.com.clinics.common.SecurityConfig.userId;

@RestController
@Tag(name = "Acesso (RBAC)")
@SecurityRequirement(name = "bearer-jwt")
public class AccessController {

    private final AccessService access;

    public AccessController(AccessService access) {
        this.access = access;
    }

    /** O app usa a lista para esconder botões; quem decide de verdade é sempre o servidor. */
    @GetMapping("/api/v1/me/access")
    @Operation(summary = "Cargo, clínica e permissões da conta logada")
    MyAccess myAccess(@AuthenticationPrincipal Jwt jwt) {
        UUID id = userId(jwt);
        String position = access.positionOf(id).map(Position::name).orElse(null);
        UUID clinicId = access.clinicIdOf(id).orElse(null);
        return new MyAccess(jwt.getClaimAsString("role"), position, clinicId, access.permissionsOf(id).stream().sorted().toList());
    }

    @GetMapping("/api/v1/admin/accounts")
    @Operation(summary = "Contas internas com cargo e clínica (conta.listar)")
    List<Account> accounts(@AuthenticationPrincipal Jwt jwt) {
        return access.accounts(userId(jwt)).stream().map(Account::from).toList();
    }

    @PutMapping("/api/v1/admin/accounts/{userId}/position")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Troca o cargo de uma conta (conta.gerenciar)")
    void changePosition(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @Valid @RequestBody ChangePositionRequest request) {
        access.changePosition(userId(jwt), userId, request.positionId(), request.clinicId());
    }

    @GetMapping("/api/v1/admin/positions")
    @Operation(summary = "Matriz de cargos e permissões (permissao.gerenciar)")
    Matrix matrix(@AuthenticationPrincipal Jwt jwt) {
        return access.matrix(userId(jwt));
    }

    @PutMapping("/api/v1/admin/positions/{positionId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Concede uma permissão ao cargo (permissao.gerenciar)")
    void grant(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID positionId, @PathVariable UUID permissionId) {
        access.grant(userId(jwt), positionId, permissionId);
    }

    @DeleteMapping("/api/v1/admin/positions/{positionId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retira uma permissão do cargo (permissao.gerenciar)")
    void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID positionId, @PathVariable UUID permissionId) {
        access.revoke(userId(jwt), positionId, permissionId);
    }

    record MyAccess(String role, String position, UUID clinicId, List<String> permissions) {
    }

    record ChangePositionRequest(@NotNull(message = "Informe o cargo.") UUID positionId, UUID clinicId) {
    }

    record Account(UUID id, String fullName, String phone, String role, String position, UUID clinicId,
            String clinicName) {

        static Account from(InternalUserRepository.AccountRow r) {
            return new Account(r.getId(), r.getFullName(), r.getPhone(), r.getRole(), r.getPosition(), r.getClinicId(),
                    r.getClinicName());
        }
    }
}
