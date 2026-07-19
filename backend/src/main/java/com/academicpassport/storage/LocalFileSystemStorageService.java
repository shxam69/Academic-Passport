package com.academicpassport.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileSystemStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileSystemStorageService.class);

    private final Path rootLocation;

    public LocalFileSystemStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Initialized storage directory at: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, Long collegeId, Long studentId, String extension) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file.");
            }
            
            // Logical key format: marksheets/{collegeId}/{studentId}/{uuid}.ext
            String filename = UUID.randomUUID().toString() + extension;
            String logicalKey = String.format("marksheets/%d/%d/%s", collegeId, studentId, filename);
            
            Path targetLocation = this.rootLocation.resolve(logicalKey).normalize();
            
            // Ensure target is within root (path traversal protection)
            if (!targetLocation.startsWith(this.rootLocation)) {
                throw new SecurityException("Cannot store file outside current directory.");
            }

            // Create parent directories
            Files.createDirectories(targetLocation.getParent());

            // Efficiently stream the file to disk
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored file at logical key: {}", logicalKey);
            return logicalKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileKey) {
        try {
            if (!StringUtils.hasText(fileKey)) {
                throw new IllegalArgumentException("File key cannot be null or empty");
            }

            Path file = rootLocation.resolve(fileKey).normalize();
            
            // Path traversal protection
            if (!file.startsWith(this.rootLocation)) {
                throw new SecurityException("Cannot access files outside storage directory.");
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + fileKey);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + fileKey, e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        try {
            if (!StringUtils.hasText(fileKey)) {
                return;
            }
            Path file = rootLocation.resolve(fileKey).normalize();
            if (!file.startsWith(this.rootLocation)) {
                log.warn("Attempted to delete file outside storage directory: {}", fileKey);
                return;
            }
            Files.deleteIfExists(file);
            log.info("Deleted file at logical key: {}", fileKey);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", fileKey, e);
            // We log but don't fail the transaction if cleanup fails
        }
    }
}
