package br.com.clinics.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.clinics.service.ReviewClinicService;
import br.com.clinics.dto.SubmitResult;
import br.com.clinics.model.PublishedReview;
import br.com.clinics.model.Review;
import br.com.clinics.dto.PageResult;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/reviews")
@Tag(name = "Avaliações")
public class ReviewController {

    private final ReviewClinicService reviews;

    public ReviewController(ReviewClinicService reviews) {
        this.reviews = reviews;
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Cria ou substitui a minha avaliação da clínica (UC10). 201 na primeira vez, 200 ao reavaliar.")
    ResponseEntity<MyReviewResponse> submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID clinicId,
            @Valid @RequestBody ReviewRequest request) {
        SubmitResult result = reviews.submit(UUID.fromString(jwt.getSubject()), clinicId, request.rating(), request.comment());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(MyReviewResponse.from(result.review()));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Consulta a minha avaliação da clínica, se existir (UC10)")
    MyReviewResponse mine(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID clinicId) {
        return MyReviewResponse.from(reviews.getMine(UUID.fromString(jwt.getSubject()), clinicId));
    }

    @GetMapping
    @Operation(summary = "Lista as avaliações da clínica, da mais recente para a mais antiga (UC10)")
    ReviewsResponse list(@PathVariable UUID clinicId, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ReviewsResponse.from(reviews.list(clinicId, page, size));
    }

    record ReviewRequest(
            @NotNull(message = "Informe a nota de 1 a 5.") Integer rating,
            @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.") String comment) {
    }

    record MyReviewResponse(UUID id, UUID clinicId, int rating, String comment, Instant createdAt, Instant updatedAt) {

        static MyReviewResponse from(Review r) {
            return new MyReviewResponse(r.id(), r.clinicId(), r.rating(), r.comment(), r.createdAt(), r.updatedAt());
        }
    }

    record PublicReview(UUID id, int rating, String comment, String authorName, Instant createdAt) {

        static PublicReview from(PublishedReview p) {
            Review r = p.review();
            return new PublicReview(r.id(), r.rating(), r.comment(), p.authorName(), r.createdAt());
        }
    }

    record ReviewsResponse(long total, int page, int size, int totalPages, List<PublicReview> data) {

        static ReviewsResponse from(PageResult<PublishedReview> result) {
            return new ReviewsResponse(result.total(), result.page(), result.size(), result.totalPages(),
                    result.items().stream().map(PublicReview::from).toList());
        }
    }
}
