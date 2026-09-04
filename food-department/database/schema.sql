-- ====================================================================
-- GovMesh - Food, Civil Supplies & Consumer Protection Department
-- PostgreSQL Database Schema (Phase 7 - Core Persistence Infrastructure)
-- ====================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100) NOT NULL,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_employee_id ON users(employee_id);

-- 2. RATION RECORDS TABLE
CREATE TABLE IF NOT EXISTS ration_records (
    id BIGSERIAL PRIMARY KEY,
    ration_card_no VARCHAR(50) NOT NULL UNIQUE,
    holder_name VARCHAR(150) NOT NULL,
    house_address TEXT NOT NULL,
    taluka_code VARCHAR(50) NOT NULL,
    district_code VARCHAR(50) NOT NULL,
    verification_flag BOOLEAN NOT NULL DEFAULT TRUE,
    update_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ration_card_no ON ration_records(ration_card_no);
CREATE INDEX IF NOT EXISTS idx_ration_district ON ration_records(district_code);

-- 3. APPLICATIONS TABLE
CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    application_id VARCHAR(50) NOT NULL UNIQUE,
    citizen_reference VARCHAR(50) NOT NULL,
    ration_card_no VARCHAR(50) NOT NULL,
    application_type VARCHAR(50) NOT NULL,
    current_status VARCHAR(50) NOT NULL,
    source_department VARCHAR(100) NOT NULL DEFAULT 'REVENUE',
    requested_address TEXT,
    officer_comments TEXT,
    reviewed_by_officer VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_application_id ON applications(application_id);
CREATE INDEX IF NOT EXISTS idx_app_citizen_ref ON applications(citizen_reference);
CREATE INDEX IF NOT EXISTS idx_app_status ON applications(current_status);

-- 4. CONSENTS TABLE
CREATE TABLE IF NOT EXISTS consents (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(50) NOT NULL UNIQUE,
    citizen_reference VARCHAR(50) NOT NULL,
    requesting_department VARCHAR(100) NOT NULL,
    receiving_department VARCHAR(100) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_consent_consent_id ON consents(consent_id);
CREATE INDEX IF NOT EXISTS idx_consent_citizen_ref ON consents(citizen_reference);
CREATE INDEX IF NOT EXISTS idx_consent_status ON consents(status);

-- 5. AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    application_id VARCHAR(50),
    officer_id BIGINT,
    action VARCHAR(100) NOT NULL,
    result VARCHAR(50) NOT NULL,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_app_id ON audit_logs(application_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);

-- 6. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_user_id);

-- 7. INTEGRATION TRANSACTIONS TABLE (GovMesh Core Interoperability Persistence)
CREATE TABLE IF NOT EXISTS integration_transactions (
    id BIGSERIAL PRIMARY KEY,
    application_id VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key VARCHAR(150),
    source_department VARCHAR(100) NOT NULL,
    target_department VARCHAR(100) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    source_protocol VARCHAR(50) NOT NULL,
    target_protocol VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_status VARCHAR(50),
    consent_status VARCHAR(50),
    consent_failure_reason TEXT,
    consent_id VARCHAR(50),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    attempt_count INT DEFAULT 1,
    max_attempts INT DEFAULT 3,
    error_code VARCHAR(100),
    error_message TEXT,
    failure_code VARCHAR(100),
    failure_message TEXT,
    raw_source_json TEXT,
    raw_canonical_json TEXT,
    raw_soap_request_xml TEXT,
    raw_soap_response_xml TEXT
);

CREATE INDEX IF NOT EXISTS idx_tx_correlation_id ON integration_transactions(correlation_id);
CREATE INDEX IF NOT EXISTS idx_tx_idempotency_key ON integration_transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_tx_status ON integration_transactions(status);
CREATE INDEX IF NOT EXISTS idx_tx_retry_status ON integration_transactions(retry_status);
CREATE INDEX IF NOT EXISTS idx_tx_next_retry_at ON integration_transactions(next_retry_at);

-- 8. INTEGRATION ATTEMPTS TABLE (Detailed Execution Attempt History)
CREATE TABLE IF NOT EXISTS integration_attempts (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(100) NOT NULL,
    application_id VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(150),
    attempt_number INT NOT NULL,
    protocol VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    failure_code VARCHAR(100),
    failure_message TEXT,
    attempt_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attempt_correlation_id ON integration_attempts(correlation_id);
CREATE INDEX IF NOT EXISTS idx_attempt_app_id ON integration_attempts(application_id);
