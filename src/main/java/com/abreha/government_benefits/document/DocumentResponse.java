package com.abreha.government_benefits.document;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DocumentResponse {

    private final Long id;
    private final Long applicationId;
    private final String originalFilename;
    private final String contentType;
    private final long sizeBytes;
    private final DocumentStatus status;
    private final String reviewNotes;
    private final LocalDateTime uploadedAt;
    private final LocalDateTime reviewedAt;

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getApplication().getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                document.getReviewNotes(),
                document.getUploadedAt(),
                document.getReviewedAt()
        );
    }
}
