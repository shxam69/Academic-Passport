-- V9: Add UNIVERSITY_ADMIN to user_role enum and sequence for college codes

ALTER TYPE user_role ADD VALUE 'UNIVERSITY_ADMIN';

-- Create sequence for auto-generating college codes (e.g., AP-000001)
CREATE SEQUENCE IF NOT EXISTS college_code_seq START 1;
