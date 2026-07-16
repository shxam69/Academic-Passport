-- V1: Enum types shared across later migrations.
-- Kept in its own migration since every other migration depends on these existing first.

CREATE TYPE user_role AS ENUM ('STUDENT', 'STAFF', 'SUPER_ADMIN');
CREATE TYPE verification_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE ticket_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
CREATE TYPE scan_status AS ENUM ('PENDING', 'CLEAN', 'INFECTED');
CREATE TYPE ocr_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED');
