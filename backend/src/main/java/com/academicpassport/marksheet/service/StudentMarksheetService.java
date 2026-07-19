package com.academicpassport.marksheet.service;

import com.academicpassport.college.Semester;
import com.academicpassport.college.SemesterRepository;
import com.academicpassport.marksheet.Marksheet;
import com.academicpassport.marksheet.MarksheetRepository;
import com.academicpassport.marksheet.MarksheetSubjectRepository;
import com.academicpassport.marksheet.OcrResult;
import com.academicpassport.marksheet.OcrResultRepository;
import com.academicpassport.marksheet.OcrStatus;
import com.academicpassport.marksheet.ScanStatus;
import com.academicpassport.storage.FileStorageService;
import com.academicpassport.student.Student;
import com.academicpassport.verification.Verification;
import com.academicpassport.verification.VerificationRepository;
import com.academicpassport.verification.VerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
public class StudentMarksheetService {

    private static final Logger log = LoggerFactory.getLogger(StudentMarksheetService.class);

    private final MarksheetRepository marksheetRepository;
    private final OcrResultRepository ocrResultRepository;
    private final MarksheetSubjectRepository marksheetSubjectRepository;
    private final VerificationRepository verificationRepository;
    private final SemesterRepository semesterRepository;
    private final FileStorageService storageService;
    private final FileValidationService validationService;
    private final ClamAVService clamAVService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public StudentMarksheetService(MarksheetRepository marksheetRepository,
                                   OcrResultRepository ocrResultRepository,
                                   MarksheetSubjectRepository marksheetSubjectRepository,
                                   VerificationRepository verificationRepository,
                                   SemesterRepository semesterRepository,
                                   FileStorageService storageService,
                                   FileValidationService validationService,
                                   ClamAVService clamAVService,
                                   org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.marksheetRepository = marksheetRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.marksheetSubjectRepository = marksheetSubjectRepository;
        this.verificationRepository = verificationRepository;
        this.semesterRepository = semesterRepository;
        this.storageService = storageService;
        this.validationService = validationService;
        this.clamAVService = clamAVService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Semester> getEligibleSemesters(Student student) {
        // Return semesters matching the student's department.
        // We prevent future semesters (e.g. current is 4, they can't upload 5 unless they somehow have a past record of it).
        // Since we don't have a strict ordering mechanism natively built in yet without assuming 'name' format, 
        // we just restrict it to their exact department for now.
        return semesterRepository.findAllByCollegeIdAndDepartmentId(student.getCollege().getId(), student.getDepartment().getId());
    }

    @Transactional
    public Marksheet uploadMarksheet(Student student, Long semesterId, MultipartFile file) {
        // 1. Validation: Semester belongs to student's department
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));

        if (!semester.getDepartment().getId().equals(student.getDepartment().getId())) {
            throw new SecurityException("Cannot upload marksheet for a different department's semester");
        }

        // 2. Prevent duplicate uploads
        if (marksheetRepository.findByStudentIdAndSemesterId(student.getId(), semesterId).isPresent()) {
            // Throw 409 Conflict at controller layer
            throw new IllegalStateException("A marksheet for this semester already exists.");
        }

        // 3. Validate File format & MIME
        String extension = validationService.validateAndGetExtension(file);

        // 4. ClamAV Malware Scan
        try {
            clamAVService.scan(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for scanning", e);
        }

        // 5. File Storage
        String fileKey = storageService.storeFile(file, student.getCollege().getId(), student.getId(), extension);
        boolean dbCommitted = false;

        try {
            // 6. Database Finalization
            Marksheet marksheet = new Marksheet();
            marksheet.setStudent(student);
            marksheet.setSemester(semester);
            marksheet.setCollege(student.getCollege());
            marksheet.setFileKey(fileKey);
            // In a real app we would hash the bytes, here we use fileKey as a placeholder
            // because we haven't implemented a hashing pass yet, but we will use dummy for MVP.
            marksheet.setFileHash(fileKey.hashCode() + ""); 
            marksheet.setUploadedAt(Instant.now());
            marksheet.setVirusScanStatus(ScanStatus.CLEAN);

            marksheet = marksheetRepository.save(marksheet);

            OcrResult ocrResult = new OcrResult();
            ocrResult.setMarksheet(marksheet);
            ocrResult.setStatus(OcrStatus.PENDING);
            ocrResult.setProcessedAt(Instant.now());

            ocrResultRepository.save(ocrResult);
            
            Verification verification = new Verification();
            verification.setMarksheet(marksheet);
            verification.setStatus(VerificationStatus.PENDING);
            verificationRepository.save(verification);
            
            dbCommitted = true;
            
            eventPublisher.publishEvent(new com.academicpassport.marksheet.event.MarksheetUploadedEvent(this, marksheet.getId()));
            
            return marksheet;

        } finally {
            // Compensating transaction: if DB fails, delete the stored file
            if (!dbCommitted) {
                log.warn("Database insert failed for marksheet. Cleaning up file: {}", fileKey);
                storageService.deleteFile(fileKey);
            }
        }
    }

    @Transactional
    public Marksheet replaceMarksheet(Student student, Long marksheetId, MultipartFile file) {
        // 1. Authorization
        Marksheet marksheet = marksheetRepository.findById(marksheetId)
                .orElseThrow(() -> new IllegalArgumentException("Marksheet not found"));

        if (!marksheet.getStudent().getId().equals(student.getId())) {
            throw new SecurityException("Cannot access another student's marksheet");
        }

        // 2. Check Verification Status (Block if VERIFIED)
        verificationRepository.findByMarksheetId(marksheetId).ifPresent(v -> {
            if (v.getStatus() == VerificationStatus.APPROVED) {
                throw new IllegalStateException("Cannot replace a verified academic record. Please contact administration.");
            }
        });

        // 3. Validate new file
        String extension = validationService.validateAndGetExtension(file);

        // 4. ClamAV Scan
        try {
            clamAVService.scan(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for scanning", e);
        }

        // 5. Store new file
        String newFileKey = storageService.storeFile(file, student.getCollege().getId(), student.getId(), extension);
        String oldFileKey = marksheet.getFileKey();
        boolean dbCommitted = false;

        try {
            // 6. DB Updates
            marksheet.setFileKey(newFileKey);
            marksheet.setFileHash(newFileKey.hashCode() + "");
            marksheet.setUploadedAt(Instant.now());
            marksheet.setVirusScanStatus(ScanStatus.CLEAN);
            Marksheet savedMarksheet = marksheetRepository.save(marksheet);
            
            // Reset OCR State
            OcrResult ocrResult = ocrResultRepository.findByMarksheetId(marksheetId)
                    .orElse(new OcrResult());
            ocrResult.setMarksheet(marksheet);
            ocrResult.setStatus(OcrStatus.PENDING);
            ocrResult.setFailureReason(null);
            ocrResult.setConfidenceScore(null);
            ocrResult.setValidationPassed(false);
            ocrResult.setValidationErrors(null);
            ocrResult.setProcessedAt(Instant.now());
            ocrResultRepository.save(ocrResult);

            Verification verification = verificationRepository.findByMarksheetId(marksheetId).orElse(new Verification());
            verification.setMarksheet(marksheet);
            verification.setStatus(VerificationStatus.PENDING);
            verificationRepository.save(verification);

            // Remove extracted subjects
            marksheetSubjectRepository.deleteByMarksheetId(marksheetId);

            dbCommitted = true;
            
            eventPublisher.publishEvent(new com.academicpassport.marksheet.event.MarksheetUploadedEvent(this, marksheet.getId()));
            
            return marksheet;

        } finally {
            if (!dbCommitted) {
                // If DB fails, delete the new file, keep old working state
                log.warn("Database replace failed. Cleaning up new file: {}", newFileKey);
                storageService.deleteFile(newFileKey);
            } else {
                // If DB succeeded, delete the old file
                log.info("Database replace succeeded. Cleaning up old file: {}", oldFileKey);
                storageService.deleteFile(oldFileKey);
            }
        }
    }

    @Transactional(readOnly = true)
    public Resource downloadMarksheet(Student student, Long marksheetId) {
        Marksheet marksheet = marksheetRepository.findById(marksheetId)
                .orElseThrow(() -> new IllegalArgumentException("Marksheet not found"));

        // Strictly enforce ownership
        if (!marksheet.getStudent().getId().equals(student.getId())) {
            // Return 404 implicitly via exception rather than 403 to prevent enumeration
            throw new IllegalArgumentException("Marksheet not found");
        }

        return storageService.loadFileAsResource(marksheet.getFileKey());
    }

    @Transactional(readOnly = true)
    public List<Marksheet> getStudentMarksheets(Student student) {
        return marksheetRepository.findAllByCollegeIdAndStudentId(student.getCollege().getId(), student.getId());
    }
}
