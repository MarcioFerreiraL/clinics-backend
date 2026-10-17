package br.com.clinics.dto;

import java.util.UUID;
import br.com.clinics.model.PublishedReview;
import br.com.clinics.model.Review;
import br.com.clinics.dto.PageResult;

public record SubmitResult(Review review, boolean created) {
}
