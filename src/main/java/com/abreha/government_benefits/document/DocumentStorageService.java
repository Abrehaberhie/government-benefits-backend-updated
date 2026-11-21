package com.abreha.government_benefits.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path storageDirectory;

    public DocumentStorageService(
            @Value("${app.documents.storage-directory:uploads/documents}")
            String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public String store(byte[] content) throws IOException {
        Files.createDirectories(storageDirectory);

        String storageKey = UUID.randomUUID().toString();

        Files.write(
                resolve(storageKey),
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return storageKey;
    }

    public byte[] read(String storageKey) throws IOException {
        return Files.readAllBytes(resolve(storageKey));
    }

    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        if (storageKey == null
                || !storageKey.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                        + "[0-9a-f]{4}-[0-9a-f]{12}"
        )) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        return storageDirectory.resolve(storageKey);
    }
}
