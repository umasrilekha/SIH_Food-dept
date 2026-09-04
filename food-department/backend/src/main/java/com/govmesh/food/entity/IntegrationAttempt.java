package com.govmesh.food.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_attempts")
public class IntegrationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "protocol", nullable = false)
    private String protocol = "SOAP/XML";

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED, TIMEOUT, SERVICE_UNAVAILABLE, BLOCKED

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "attempt_at", nullable = false)
    private LocalDateTime attemptAt;

    public IntegrationAttempt() {}

    public IntegrationAttempt(Long id, String correlationId, String applicationId, String idempotencyKey, Integer attemptNumber, String protocol, String status, String failureCode, String failureMessage, LocalDateTime attemptAt) {
        this.id = id;
        this.correlationId = correlationId;
        this.applicationId = applicationId;
        this.idempotencyKey = idempotencyKey;
        this.attemptNumber = attemptNumber;
        this.protocol = protocol != null ? protocol : "SOAP/XML";
        this.status = status;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.attemptAt = attemptAt != null ? attemptAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public LocalDateTime getAttemptAt() { return attemptAt; }
    public void setAttemptAt(LocalDateTime attemptAt) { this.attemptAt = attemptAt; }

    public static class Builder {
        private Long id;
        private String correlationId;
        private String applicationId;
        private String idempotencyKey;
        private Integer attemptNumber;
        private String protocol = "SOAP/XML";
        private String status;
        private String failureCode;
        private String failureMessage;
        private LocalDateTime attemptAt = LocalDateTime.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder applicationId(String applicationId) { this.applicationId = applicationId; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder attemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; return this; }
        public Builder protocol(String protocol) { this.protocol = protocol; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder failureCode(String failureCode) { this.failureCode = failureCode; return this; }
        public Builder failureMessage(String failureMessage) { this.failureMessage = failureMessage; return this; }
        public Builder attemptAt(LocalDateTime attemptAt) { this.attemptAt = attemptAt; return this; }

        public IntegrationAttempt build() {
            return new IntegrationAttempt(id, correlationId, applicationId, idempotencyKey, attemptNumber, protocol, status, failureCode, failureMessage, attemptAt);
        }
    }
}
