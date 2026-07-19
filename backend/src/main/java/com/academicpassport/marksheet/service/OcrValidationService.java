package com.academicpassport.marksheet.service;

import com.academicpassport.college.Subject;
import com.academicpassport.college.SubjectRepository;
import com.academicpassport.marksheet.ExtractionFinding;
import com.academicpassport.marksheet.Marksheet;
import com.academicpassport.marksheet.dto.ExtractionResult;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OcrValidationService {

    private final SubjectRepository subjectRepository;
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public OcrValidationService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public OcrValidationResult validateAndMatch(ExtractionResult extraction, Marksheet marksheet) {
        OcrValidationResult result = new OcrValidationResult();
        double score = 100.0;

        // 1. Validate Student Register Number
        if (extraction.getRegisterNumber() != null) {
            String extractedReg = normalize(extraction.getRegisterNumber());
            String expectedReg = normalize(marksheet.getStudent().getUniversityRegisterNo());
            if (!extractedReg.equals(expectedReg)) {
                result.addFinding(ExtractionFinding.REGISTER_NUMBER_MISMATCH);
                score -= 20.0;
            }
        } else {
            result.addFinding(ExtractionFinding.UNREADABLE_VALUE);
            score -= 10.0;
        }

        // 2. Validate Semester
        if (extraction.getSemesterNumber() != null) {
            if (!extraction.getSemesterNumber().equals(marksheet.getSemester().getSemesterNumber())) {
                result.addFinding(ExtractionFinding.SEMESTER_MISMATCH);
                score -= 20.0;
            }
        }

        // 3. Match Subjects
        List<Subject> expectedSubjects = subjectRepository.findAllByCollegeIdAndSemesterId(
            marksheet.getCollege().getId(), 
            marksheet.getSemester().getId()
        );
        Set<Long> matchedSubjectIds = new HashSet<>();
        
        if (extraction.getSubjects() == null) {
            extraction.setSubjects(new ArrayList<>());
        }

        for (ExtractionResult.ExtractedSubject extractedSub : extraction.getSubjects()) {
            Subject bestMatch = null;
            boolean isExact = false;

            // Step A: Exact Code Match
            if (extractedSub.getSubjectCode() != null) {
                String normCode = normalize(extractedSub.getSubjectCode());
                for (Subject sub : expectedSubjects) {
                    if (normalize(sub.getSubjectCode()).equals(normCode)) {
                        bestMatch = sub;
                        isExact = true;
                        break;
                    }
                }
            }

            // Step B: Exact Name Match
            if (bestMatch == null && extractedSub.getSubjectName() != null) {
                String normName = normalize(extractedSub.getSubjectName());
                for (Subject sub : expectedSubjects) {
                    if (normalize(sub.getSubjectName()).equals(normName)) {
                        bestMatch = sub;
                        isExact = true;
                        break;
                    }
                }
            }

            // Step C: Fuzzy Match
            if (bestMatch == null && extractedSub.getSubjectName() != null) {
                String extractedName = normalize(extractedSub.getSubjectName());
                Subject candidate = null;
                double bestRatio = 0.0;
                double secondBestRatio = 0.0;

                for (Subject sub : expectedSubjects) {
                    String expectedName = normalize(sub.getSubjectName());
                    int distance = levenshtein.apply(extractedName, expectedName);
                    int maxLength = Math.max(extractedName.length(), expectedName.length());
                    double ratio = 1.0 - ((double) distance / maxLength);

                    if (ratio > bestRatio) {
                        secondBestRatio = bestRatio;
                        bestRatio = ratio;
                        candidate = sub;
                    } else if (ratio > secondBestRatio) {
                        secondBestRatio = ratio;
                    }
                }

                if (bestRatio >= 0.85) {
                    if ((bestRatio - secondBestRatio) >= 0.05) {
                        bestMatch = candidate;
                        score -= 5.0; // penalty for fuzzy
                    } else {
                        result.addFinding(ExtractionFinding.AMBIGUOUS_SUBJECT);
                    }
                }
            }

            if (bestMatch != null) {
                if (matchedSubjectIds.contains(bestMatch.getId())) {
                    result.addFinding(ExtractionFinding.DUPLICATE_SUBJECT);
                    score -= 10.0;
                } else {
                    matchedSubjectIds.add(bestMatch.getId());
                    OcrValidationResult.MatchedSubject matched = new OcrValidationResult.MatchedSubject();
                    matched.setExpectedSubject(bestMatch);
                    matched.setExtracted(extractedSub);
                    matched.setExactMatch(isExact);
                    result.getMatchedSubjects().add(matched);

                    // Validate Marks
                    if (extractedSub.getMarksObtained() != null) {
                        if (extractedSub.getMarksObtained() < 0 || extractedSub.getMarksObtained() > bestMatch.getMaxMarks()) {
                            result.addFinding(ExtractionFinding.INVALID_MARK_RANGE);
                            score -= 20.0;
                        }
                    } else {
                        // Unreadable marks
                        result.addFinding(ExtractionFinding.UNREADABLE_VALUE);
                    }
                }
            } else {
                result.addFinding(ExtractionFinding.UNKNOWN_SUBJECT);
                score -= 10.0;
            }
        }

        // Check for missing subjects
        for (Subject expected : expectedSubjects) {
            if (!matchedSubjectIds.contains(expected.getId())) {
                result.addFinding(ExtractionFinding.MISSING_EXPECTED_SUBJECT);
                score -= 10.0;
            }
        }
        
        result.setConfidenceScore(BigDecimal.valueOf(Math.max(0.0, score)));
        if (score < 80.0) {
            result.setReviewRequired(true);
        }

        return result;
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
