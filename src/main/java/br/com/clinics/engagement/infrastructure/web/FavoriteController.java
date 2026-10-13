package br.com.clinics.engagement.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.clinics.engagement.application.port.in.ManageFavoritesUseCase;
import br.com.clinics.engagement.domain.FavoriteExam;
import br.com.clinics.shared.domain.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/favorites")
@Tag(name = "Favoritos")
@SecurityRequirement(name = "bearer-jwt")
class FavoriteController {

    private final ManageFavoritesUseCase favorites;

    FavoriteController(ManageFavoritesUseCase favorites) {
        this.favorites = favorites;
    }

    @PutMapping("/{examTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Favorita um tipo de exame; repetir não tem efeito (UC09)")
    void add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID examTypeId) {
        favorites.add(UUID.fromString(jwt.getSubject()), examTypeId);
    }

    @DeleteMapping("/{examTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove dos favoritos; repetir não tem efeito (UC09)")
    void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID examTypeId) {
        favorites.remove(UUID.fromString(jwt.getSubject()), examTypeId);
    }

    @GetMapping
    @Operation(summary = "Lista os favoritos com o menor preço atual (UC09)")
    FavoritesResponse list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return FavoritesResponse.from(favorites.list(UUID.fromString(jwt.getSubject()), page, size));
    }

    record FavoriteItem(UUID examTypeId, String name, String slug, String category, BigDecimal minPrice,
            int offersCount, Instant favoritedAt) {

        static FavoriteItem from(FavoriteExam f) {
            return new FavoriteItem(f.examTypeId(), f.name(), f.slug(), f.category().label(), f.minPrice(),
                    f.offersCount(), f.favoritedAt());
        }
    }

    record FavoritesResponse(long total, int page, int size, int totalPages, List<FavoriteItem> data) {

        static FavoritesResponse from(PageResult<FavoriteExam> result) {
            return new FavoritesResponse(result.total(), result.page(), result.size(), result.totalPages(),
                    result.items().stream().map(FavoriteItem::from).toList());
        }
    }
}
