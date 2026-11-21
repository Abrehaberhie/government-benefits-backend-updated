package com.abreha.government_benefits.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {

    List<Document> findByApplication_IdOrderByUploadedAtDesc(
            Long applicationId
    );

    Optional<Document> findByIdAndApplication_User_Email(
            Long documentId,
            String email
    );
}