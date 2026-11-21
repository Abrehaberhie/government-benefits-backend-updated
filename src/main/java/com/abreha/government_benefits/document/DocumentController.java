package com.abreha.government_benefits.document;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(
            value = "/application/{applicationId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file
    ) {
        return documentService.upload(applicationId, file);
    }

    @GetMapping("/application/{applicationId}")
    public List<DocumentResponse> getByApplication(
            @PathVariable Long applicationId
    ) {
        return documentService.getByApplication(applicationId);
    }

    @PutMapping("/{documentId}/review")
    public DocumentResponse review(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentReviewRequest request
    ) {
        return documentService.review(documentId, request);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long documentId
    ) {
        DocumentService.DownloadedDocument document =
                documentService.download(documentId);

        String disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(document.contentType())
                )
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(document.content());
    }

    @PostMapping(
            value = "/mine/application/{applicationId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadMyDocument(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file,
            java.security.Principal principal
    ) {
        return documentService.uploadMyDocument(
                applicationId,
                file,
                principal.getName()
        );
    }

    @GetMapping("/mine/application/{applicationId}")
    public List<DocumentResponse> getMyDocuments(
            @PathVariable Long applicationId,
            java.security.Principal principal
    ) {
        return documentService.getMyDocuments(
                applicationId,
                principal.getName()
        );
    }

    @GetMapping("/mine/{documentId}/download")
    public ResponseEntity<byte[]> downloadMyDocument(
            @PathVariable Long documentId,
            java.security.Principal principal
    ) {
        DocumentService.DownloadedDocument document =
                documentService.downloadMyDocument(
                        documentId,
                        principal.getName()
                );

        String disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(document.contentType())
                )
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(document.content());
    }
}
