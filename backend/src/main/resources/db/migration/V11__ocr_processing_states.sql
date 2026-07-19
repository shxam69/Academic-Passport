-- V11__ocr_processing_states.sql
-- Add new OCR states for processing and retry logic

ALTER TYPE ocr_status ADD VALUE 'PROCESSING';
ALTER TYPE ocr_status ADD VALUE 'FAILED_RETRYABLE';

-- Add metadata fields for background processing
ALTER TABLE ocr_results 
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN processing_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_attempt_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN findings JSONB;
