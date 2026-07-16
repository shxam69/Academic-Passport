package com.academicpassport.marksheet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

    // No direct tenant scoping here — OcrResult has no college_id of its own by
    // design (it's a 1:1 child of Marksheet, which is already tenant-scoped).
    // The service layer must load and ownership-check the parent Marksheet
    // (findByIdAndCollegeId) BEFORE calling this — never look up an OcrResult by
    // marksheetId alone and trust it without that prior check, or a student could
    // read another college's OCR data by guessing marksheet ids.
    Optional<OcrResult> findByMarksheetId(Long marksheetId);
}
