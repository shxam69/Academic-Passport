-- V12__fix_marksheet_subjects_audit.sql
-- Fixes schema gap to explicitly separate AI-extracted, corrected, and verified marks

ALTER TABLE marksheet_subjects RENAME COLUMN marks_obtained TO corrected_marks;

ALTER TABLE marksheet_subjects ADD COLUMN ai_extracted_marks INTEGER;
ALTER TABLE marksheet_subjects ADD COLUMN verified_marks INTEGER;

-- Migrate any existing marks (if any) to act as both AI extracted and corrected initially
UPDATE marksheet_subjects SET ai_extracted_marks = corrected_marks WHERE corrected_marks IS NOT NULL;
