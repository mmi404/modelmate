package com.modelmate.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class ModelDtos {

    private ModelDtos() {
    }

    /** Minimal identity for dropdowns / typeahead. */
    public record ModelSummaryDto(Long id, String name, String slug, String creator) {
    }

    public record CategoryRef(String slug, String name) {
    }

    public record UserRef(Long id, String name) {
    }

    /** Averaged ratings over a model's visible reviews. */
    public record RatingSummary(
            BigDecimal overall,
            Double accuracy,
            Double speed,
            Double cost,
            Double easeOfUse,
            Double reliability,
            long reviewCount
    ) {
        public static RatingSummary empty() {
            return new RatingSummary(null, null, null, null, null, null, 0);
        }
    }

    /** List/grid card. */
    public record ModelCardDto(
            Long id,
            String name,
            String slug,
            String creator,
            CategoryRef category,
            String description,
            RatingSummary ratings
    ) {
    }

    /** Full model detail page payload. */
    public record ModelDetailDto(
            Long id,
            String name,
            String slug,
            String creator,
            String description,
            String websiteUrl,
            String logoUrl,
            CategoryRef category,
            UserRef submitter,
            Instant createdAt,
            RatingSummary ratings,
            long problemCount
    ) {
    }

    public record SubmitModelRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String creator,
            @NotNull Long categoryId,
            @Size(max = 5000) String description,
            @NotBlank @Size(max = 500) String websiteUrl
    ) {
    }

    public record SubmissionAcceptedDto(Long submissionId, String status, String message) {
    }
}
