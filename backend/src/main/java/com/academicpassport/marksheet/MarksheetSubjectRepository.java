package com.academicpassport.marksheet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarksheetSubjectRepository extends JpaRepository<MarksheetSubject, Long> {

    // Same ownership-check discipline as OcrResultRepository: the service layer
    // must load+verify the parent Marksheet's tenant/ownership before calling this.
    List<MarksheetSubject> findAllByMarksheetId(Long marksheetId);
    
    java.util.Optional<MarksheetSubject> findByMarksheetIdAndSubjectId(Long marksheetId, Long subjectId);

    void deleteByMarksheetId(Long marksheetId);
}
