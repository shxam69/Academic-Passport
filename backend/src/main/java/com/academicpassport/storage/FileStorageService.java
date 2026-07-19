package com.academicpassport.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    
    /**
     * Stores the file securely and returns a logical file key.
     * Preferred format: marksheets/{collegeId}/{studentId}/{uuid}.ext
     */
    String storeFile(MultipartFile file, Long collegeId, Long studentId, String extension);
    
    /**
     * Loads a file as a resource using its logical key.
     */
    Resource loadFileAsResource(String fileKey);
    
    /**
     * Deletes a file securely.
     */
    void deleteFile(String fileKey);
}
