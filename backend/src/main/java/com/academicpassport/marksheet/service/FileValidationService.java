package com.academicpassport.marksheet.service;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class FileValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);
    
    // 5 MB limit
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final Tika tika;

    public FileValidationService() {
        this.tika = new Tika();
    }

    /**
     * Validates file size, extension, and magic-byte MIME type.
     * Throws IllegalArgumentException on failure.
     * Returns the detected extension.
     */
    public String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        String extension = getExtension(originalFilename);
        if (!isExtensionAllowed(extension)) {
            throw new IllegalArgumentException("Unsupported file extension: " + extension);
        }

        try (InputStream is = file.getInputStream()) {
            String detectedMime = tika.detect(is, originalFilename);
            log.info("Detected MIME type: {} for file: {}", detectedMime, originalFilename);

            if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
                throw new IllegalArgumentException("Unsupported file content type: " + detectedMime);
            }

            // Cross-check extension and MIME
            if (!isMimeAndExtensionConsistent(detectedMime, extension)) {
                throw new IllegalArgumentException("File extension does not match its contents");
            }

            return extension;
        } catch (IOException e) {
            log.error("Failed to read file for validation", e);
            throw new RuntimeException("Failed to validate file", e);
        }
    }

    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    private boolean isExtensionAllowed(String ext) {
        return ext.equals(".pdf") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
    }

    private boolean isMimeAndExtensionConsistent(String mime, String ext) {
        if (mime.equals("application/pdf")) {
            return ext.equals(".pdf");
        }
        if (mime.equals("image/jpeg")) {
            return ext.equals(".jpg") || ext.equals(".jpeg");
        }
        if (mime.equals("image/png")) {
            return ext.equals(".png");
        }
        return false;
    }
}
