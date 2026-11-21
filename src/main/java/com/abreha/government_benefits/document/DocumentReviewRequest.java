package com.abreha.government_benefits.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentReviewRequest {

    @NotNull(message = "Document status is required")
    private DocumentStatus status;

    @Size(
            max = 1000,
            message = "Review notes cannot exceed 1000 characters"
    )
    private String reviewNotes;
}
