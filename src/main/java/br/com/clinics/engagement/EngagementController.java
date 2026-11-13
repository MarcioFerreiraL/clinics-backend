package br.com.clinics.engagement;

import java.util.UUID;

import br.com.clinics.common.Page;
import br.com.clinics.engagement.EngagementService.FavoriteItem;
import br.com.clinics.engagement.EngagementService.PublicReview;
import br.com.clinics.engagement.EngagementService.MyReview;
import br.com.clinics.engagement.EngagementService.SavingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static br.com.clinics.common.SecurityConfig.userId;

@RestController
@Tag(name = "Favoritos, economia e avaliações")
public class EngagementController {

    private final EngagementService engagement;

    public EngagementController(EngagementService engagement) {
        this.engagement = engagement;
    }

    @PutMapping("/api/v1/me/favorites/{examTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Favorita um tipo de exame")
    void addFavorite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID examTypeId) {
        engagement.addFavorite(userId(jwt), examTypeId);
    }

    @DeleteMapping("/api/v1/me/favorites/{examTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Remove dos favoritos")
    void removeFavorite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID examTypeId) {
        engagement.removeFavorite(userId(jwt), examTypeId);
    }

    @GetMapping("/api/v1/me/favorites")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Lista os favoritos com o menor preço atual")
    Page<FavoriteItem> favorites(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return engagement.favorites(userId(jwt), page, size);
    }

    @GetMapping("/api/v1/me/savings")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Total economizado e número de contatos")
    SavingsResponse savings(@AuthenticationPrincipal Jwt jwt) {
        return engagement.savings(userId(jwt));
    }

    @PutMapping("/api/v1/clinics/{clinicId}/reviews/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Cria ou substitui a minha avaliação (201 na primeira vez, 200 ao reavaliar)")
    ResponseEntity<MyReview> submitReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID clinicId,
            @Valid @RequestBody ReviewRequest request) {
        var result = engagement.submitReview(userId(jwt), clinicId, request.rating(), request.comment());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.review());
    }

    @GetMapping("/api/v1/clinics/{clinicId}/reviews/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Consulta a minha avaliação da clínica")
    MyReview myReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID clinicId) {
        return engagement.myReview(userId(jwt), clinicId);
    }

    @GetMapping("/api/v1/clinics/{clinicId}/reviews")
    @Operation(summary = "Lista as avaliações da clínica, da mais recente para a mais antiga")
    Page<PublicReview> reviews(@PathVariable UUID clinicId, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return engagement.reviews(clinicId, page, size);
    }

    record ReviewRequest(
            @NotNull(message = "Informe a nota de 1 a 5.") Integer rating,
            @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.") String comment) {
    }

}
