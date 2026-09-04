package com.govmesh.food.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_transactions")
public class IntegrationTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "correlation_id", nullable = false, unique = true)
    private String correlationId;

    @Column(name = "source_department", nullable = false)
    private String sourceDepartment;

    @Column(name = "target_department", nullable = false)
    private String targetDepartment;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "source_protocol", nullable = false)
    private String sourceProtocol;

    @Column(name = "target_protocol", nullable = false)
    private String targetProtocol;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "consent_status")
    private String consentStatus; // ALLOWED, BLOCKED

    @Column(name = "consent_failure_reason")
    private String consentFailureReason;

    @Column(name = "consent_id")
    private String consentId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "raw_source_json", columnDefinition = "TEXT")
    private String rawSourceJson;

    @Column(name = "raw_canonical_json", columnDefinition = "TEXT")
    private String rawCanonicalJson;

    @Column(name = "raw_soap_request_xml", columnDefinition = "TEXT")
    private String rawSoapRequestXml;

    @Column(name = "raw_soap_response_xml", columnDefinition = "TEXT")
    private String rawSoapResponseXml;

    // Phase 6 Reliability & Idempotency Fields
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "attempt_count")
    private Integer attemptCount = 1;

    @Column(name = "max_attempts")
    private Integer maxAttempts = 3;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "retry_status")
    private String retryStatus;

    @Column(name = "is_duplicate")
    private Boolean isDuplicate = false;

    public IntegrationTransaction() {}

    public IntegrationTransaction(Long id, String applicationId, String correlationId, String sourceDepartment, String targetDepartment, String operation, String sourceProtocol, String targetProtocol, String status, String consentStatus, String consentFailureReason, String consentId, LocalDateTime startedAt, LocalDateTime completedAt, String errorCode, String errorMessage, String rawSourceJson, String rawCanonicalJson, String rawSoapRequestXml, String rawSoapResponseXml) {
        this.id = id;
        this.applicationId = applicationId;
        this.correlationId = correlationId;
        this.sourceDepartment = sourceDepartment;
        this.targetDepartment = targetDepartment;
        this.operation = operation;
        this.sourceProtocol = sourceProtocol;
        this.targetProtocol = targetProtocol;
        this.status = status;
        this.consentStatus = consentStatus;
        this.consentFailureReason = consentFailureReason;
        this.consentId = consentId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.rawSourceJson = rawSourceJson;
        this.rawCanonicalJson = rawCanonicalJson;
        this.rawSoapRequestXml = rawSoapRequestXml;
        this.rawSoapResponseXml = rawSoapResponseXml;
        this.retryStatus = status;
    }

    public IntegrationTransaction(Long id, String applicationId, String correlationId, String sourceDepartment, String targetDepartment, String operation, String sourceProtocol, String targetProtocol, String status, String consentStatus, String consentFailureReason, String consentId, LocalDateTime startedAt, LocalDateTime completedAt, String errorCode, String errorMessage, String rawSourceJson, String rawCanonicalJson, String rawSoapRequestXml, String rawSoapResponseXml, String idempotencyKey, Integer attemptCount, Integer maxAttempts, LocalDateTime lastAttemptAt, LocalDateTime nextRetryAt, String failureCode, String failureMessage, String retryStatus, Boolean isDuplicate) {
        this.id = id;
        this.applicationId = applicationId;
        this.correlationId = correlationId;
        this.sourceDepartment = sourceDepartment;
        this.targetDepartment = targetDepartment;
        this.operation = operation;
        this.sourceProtocol = sourceProtocol;
        this.targetProtocol = targetProtocol;
        this.status = status;
        this.consentStatus = consentStatus;
        this.consentFailureReason = consentFailureReason;
        this.consentId = consentId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.rawSourceJson = rawSourceJson;
        this.rawCanonicalJson = rawCanonicalJson;
        this.rawSoapRequestXml = rawSoapRequestXml;
        this.rawSoapResponseXml = rawSoapResponseXml;
        this.idempotencyKey = idempotencyKey;
        this.attemptCount = attemptCount != null ? attemptCount : 1;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.retryStatus = retryStatus != null ? retryStatus : status;
        this.isDuplicate = isDuplicate != null ? isDuplicate : false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSourceDepartment() { return sourceDepartment; }
    public void setSourceDepartment(String sourceDepartment) { this.sourceDepartment = sourceDepartment; }

    public String getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getSourceProtocol() { return sourceProtocol; }
    public void setSourceProtocol(String sourceProtocol) { this.sourceProtocol = sourceProtocol; }

    public String getTargetProtocol() { return targetProtocol; }
    public void setTargetProtocol(String targetProtocol) { this.targetProtocol = targetProtocol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConsentStatus() { return consentStatus; }
    public void setConsentStatus(String consentStatus) { this.consentStatus = consentStatus; }

    public String getConsentFailureReason() { return consentFailureReason; }
    public void setConsentFailureReason(String consentFailureReason) { this.consentFailureReason = consentFailureReason; }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getRawSourceJson() { return rawSourceJson; }
    public void setRawSourceJson(String rawSourceJson) { this.rawSourceJson = rawSourceJson; }

    public String getRawCanonicalJson() { return rawCanonicalJson; }
    public void setRawCanonicalJson(String rawCanonicalJson) { this.rawCanonicalJson = rawCanonicalJson; }

    public String getRawSoapRequestXml() { return rawSoapRequestXml; }
    public void setRawSoapRequestXml(String rawSoapRequestXml) { this.rawSoapRequestXml = rawSoapRequestXml; }

    public String getRawSoapResponseXml() { return rawSoapResponseXml; }
    public void setRawSoapResponseXml(String rawSoapResponseXml) { this.rawSoapResponseXml = rawSoapResponseXml; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public String getRetryStatus() { return retryStatus; }
    public void setRetryStatus(String retryStatus) { this.retryStatus = retryStatus; }

    public Boolean getIsDuplicate() { return isDuplicate; }
    public void setIsDuplicate(Boolean isDuplicate) { this.isDuplicate = isDuplicate; }

    public static class Builder {
        private Long id;
        private String applicationId;
        private String correlationId;
        private String sourceDepartment;
        private String targetDepartment;
        private String operation;
        private String sourceProtocol;
        private String targetProtocol;
        private String status;
        private String consentStatus;
        private String consentFailureReason;
        private String consentId;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorCode;
        private String errorMessage;
        private String rawSourceJson;
        private String rawCanonicalJson;
        private String rawSoapRequestXml;
        private String rawSoapResponseXml;
        private String idempotencyKey;
        private Integer attemptCount = 1;
        private Integer maxAttempts = 3;
        private LocalDateTime lastAttemptAt;
        private LocalDateTime nextRetryAt;
        private String failureCode;
        private String failureMessage;
        private String retryStatus;
        private Boolean isDuplicate = false;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder applicationId(String applicationId) { this.applicationId = applicationId; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder sourceDepartment(String sourceDepartment) { this.sourceDepartment = sourceDepartment; return this; }
        public Builder targetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; return this; }
        public Builder operation(String operation) { this.operation = operation; return this; }
        public Builder sourceProtocol(String sourceProtocol) { this.sourceProtocol = sourceProtocol; return this; }
        public Builder targetProtocol(String targetProtocol) { this.targetProtocol = targetProtocol; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder consentStatus(String consentStatus) { this.consentStatus = consentStatus; return this; }
        public Builder consentFailureReason(String consentFailureReason) { this.consentFailureReason = consentFailureReason; return this; }
        public Builder consentId(String consentId) { this.consentId = consentId; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder rawSourceJson(String rawSourceJson) { this.rawSourceJson = rawSourceJson; return this; }
        public Builder rawCanonicalJson(String rawCanonicalJson) { this.rawCanonicalJson = rawCanonicalJson; return this; }
        public Builder rawSoapRequestXml(String rawSoapRequestXml) { this.rawSoapRequestXml = rawSoapRequestXml; return this; }
        public Builder rawSoapResponseXml(String rawSoapResponseXml) { this.rawSoapResponseXml = rawSoapResponseXml; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder attemptCount(Integer attemptCount) { this.attemptCount = attemptCount; return this; }
        public Builder maxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public Builder lastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; return this; }
        public Builder nextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; return this; }
        public Builder failureCode(String failureCode) { this.failureCode = failureCode; return this; }
        public Builder failureMessage(String failureMessage) { this.failureMessage = failureMessage; return this; }
        public Builder retryStatus(String retryStatus) { this.retryStatus = retryStatus; return this; }
        public Builder isDuplicate(Boolean isDuplicate) { this.isDuplicate = isDuplicate; return this; }

        public IntegrationTransaction build() {
            return new IntegrationTransaction(id, applicationId, correlationId, sourceDepartment, targetDepartment, operation, sourceProtocol, targetProtocol, status, consentStatus, consentFailureReason, consentId, startedAt, completedAt, errorCode, errorMessage, rawSourceJson, rawCanonicalJson, rawSoapRequestXml, rawSoapResponseXml, idempotencyKey, attemptCount, maxAttempts, lastAttemptAt, nextRetryAt, failureCode, failureMessage, retryStatus, isDuplicate);
        }
    }
}
