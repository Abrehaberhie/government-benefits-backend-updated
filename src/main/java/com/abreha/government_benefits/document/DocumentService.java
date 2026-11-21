package com.abreha.government_benefits.document;

import com.abreha.government_benefits.application.Application;
import com.abreha.government_benefits.application.ApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentStorageService storageService;

    public DocumentService(
            DocumentRepository documentRepository,
            ApplicationRepository applicationRepository,
            DocumentStorageService storageService
    ) {
        this.documentRepository = documentRepository;
        this.applicationRepository = applicationRepository;
        this.storageService = storageService;
    }

    @Transactional
    public DocumentResponse upload(
            Long applicationId,
            MultipartFile file
    ) {
        Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select a nonempty file"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File must not exceed 5 MB"
            );
        }

        byte[] content;

        try (InputStream input = file.getInputStream()) {
            content = input.readNBytes(MAX_FILE_SIZE + 1);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read uploaded file",
                    exception
            );
        }

        if (content.length > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File must not exceed 5 MB"
            );
        }

        String contentType = detectContentType(content);
        String filename = cleanFilename(file.getOriginalFilename());
        String storageKey;

        try {
            storageKey = storageService.store(content);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store uploaded file",
                    exception
            );
        }

        // Remove the stored file if the database transaction rolls back.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try {
                                storageService.delete(storageKey);
                            } catch (IOException exception) {
                                org.slf4j.LoggerFactory
                                        .getLogger(DocumentService.class)
                                        .error(
                                                "Could not remove rolled-back upload {}",
                                                storageKey,
                                                exception
                                        );
                            }
                        }
                    }
                }
        );

        Document document = new Document();
        document.setApplication(application);
        document.setOriginalFilename(filename);
        document.setStorageKey(storageKey);
        document.setContentType(contentType);
        document.setSizeBytes(content.length);
        document.setStatus(DocumentStatus.PENDING);

        return DocumentResponse.from(
                documentRepository.saveAndFlush(document)
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getByApplication(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application not found"
            );
        }

        return documentRepository
                .findByApplication_IdOrderByUploadedAtDesc(applicationId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public DocumentResponse review(
            Long documentId,
            DocumentReviewRequest request
    ) {
        DocumentStatus status = request.getStatus();

        if (status != DocumentStatus.VERIFIED
                && status != DocumentStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Review status must be VERIFIED or REJECTED"
            );
        }

        String notes = request.getReviewNotes();

        if (status == DocumentStatus.REJECTED
                && (notes == null || notes.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A reason is required when rejecting a document"
            );
        }

        Document document = findDocument(documentId);
        document.setStatus(status);
        document.setReviewNotes(notes == null ? null : notes.trim());
        document.setReviewedAt(LocalDateTime.now());

        return DocumentResponse.from(
                documentRepository.save(document)
        );
    }

    @Transactional(readOnly = true)
    public DownloadedDocument download(Long documentId) {
        Document document = findDocument(documentId);

        try {
            return new DownloadedDocument(
                    document.getOriginalFilename(),
                    document.getContentType(),
                    storageService.read(document.getStorageKey())
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read stored document",
                    exception
            );
        }
    }

    private Document findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));
    }

    private String cleanFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }

        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        filename = filename.replaceAll("[\\p{Cntrl}]", "_").trim();

        if (filename.isBlank()) {
            return "document";
        }

        return filename.substring(0, Math.min(filename.length(), 200));
    }

    private String detectContentType(byte[] content) {
        if (content.length >= 5
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-') {
            return "application/pdf";
        }

        if (content.length >= 8
                && (content[0] & 0xff) == 0x89
                && content[1] == 'P'
                && content[2] == 'N'
                && content[3] == 'G'
                && content[4] == 13
                && content[5] == 10
                && content[6] == 26
                && content[7] == 10) {
            return "image/png";
        }

        if (content.length >= 3
                && (content[0] & 0xff) == 0xff
                && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only PDF, PNG, and JPEG files are supported"
        );
    }

    public record DownloadedDocument(
            String filename,
            String contentType,
            byte[] content
    ) {
    }

    @Transactional
    public DocumentResponse uploadMyDocument(
            Long applicationId,
            MultipartFile file,
            String email
    ) {
        applicationRepository.findByIdAndUser_Email(applicationId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));

        return upload(applicationId, file);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(
            Long applicationId,
            String email
    ) {
        applicationRepository.findByIdAndUser_Email(applicationId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));

        return getByApplication(applicationId);
    }

    @Transactional(readOnly = true)
    public DownloadedDocument downloadMyDocument(
            Long documentId,
            String email
    ) {
        Document document = documentRepository
                .findByIdAndApplication_User_Email(documentId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        try {
            return new DownloadedDocument(
                    document.getOriginalFilename(),
                    document.getContentType(),
                    storageService.read(document.getStorageKey())
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read stored document",
                    exception
            );
        }
    }
}
