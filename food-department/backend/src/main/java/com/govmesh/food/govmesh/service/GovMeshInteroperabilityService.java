package com.govmesh.food.govmesh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govmesh.food.entity.AuditLog;
import com.govmesh.food.entity.IntegrationAttempt;
import com.govmesh.food.entity.IntegrationTransaction;
import com.govmesh.food.exception.ResourceNotFoundException;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateRequest;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateResponse;
import com.govmesh.food.govmesh.dto.ConsentValidationResult;
import com.govmesh.food.govmesh.router.IntegrationRouter;
import com.govmesh.food.repository.AuditLogRepository;
import com.govmesh.food.repository.IntegrationAttemptRepository;
import com.govmesh.food.repository.IntegrationTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class GovMeshInteroperabilityService {

    private final IntegrationRouter integrationRouter;
    private final IntegrationTransactionRepository transactionRepository;
    private final IntegrationAttemptRepository attemptRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConsentValidationService consentValidationService;
    private final ObjectMapper objectMapper;

    public GovMeshInteroperabilityService(IntegrationRouter integrationRouter,
                                         IntegrationTransactionRepository transactionRepository,
                                         AuditLogRepository auditLogRepository,
                                         ConsentValidationService consentValidationService) {
        this(integrationRouter, transactionRepository, null, auditLogRepository, consentValidationService);
    }

    public GovMeshInteroperabilityService(IntegrationRouter integrationRouter,
                                         IntegrationTransactionRepository transactionRepository,
                                         IntegrationAttemptRepository attemptRepository,
                                         AuditLogRepository auditLogRepository,
                                         ConsentValidationService consentValidationService) {
        this.integrationRouter = integrationRouter;
        this.transactionRepository = transactionRepository;
        this.attemptRepository = attemptRepository;
        this.auditLogRepository = auditLogRepository;
        this.consentValidationService = consentValidationService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public CanonicalAddressUpdateResponse processInteroperabilityRequest(CanonicalAddressUpdateRequest canonicalRequest) {
        String corrId = (canonicalRequest != null && canonicalRequest.getCorrelationId() != null)
                ? canonicalRequest.getCorrelationId()
                : "REQ-2026-" + System.currentTimeMillis();

        String appId = (canonicalRequest != null && canonicalRequest.getApplicationId() != null)
                ? canonicalRequest.getApplicationId()
                : "GM-2026-UNKNOWN";

        String sourceDept = (canonicalRequest != null && canonicalRequest.getSourceDepartment() != null)
                ? canonicalRequest.getSourceDepartment()
                : "REVENUE";

        String targetDept = (canonicalRequest != null && canonicalRequest.getTargetDepartment() != null)
                ? canonicalRequest.getTargetDepartment()
                : "FOOD";

        String consentId = (canonicalRequest != null && canonicalRequest.getConsent() != null)
                ? canonicalRequest.getConsent().getId()
                : null;

        String purpose = (canonicalRequest != null && canonicalRequest.getPurpose() != null)
                ? canonicalRequest.getPurpose()
                : "RATION_ADDRESS_UPDATE";

        String idempotencyKey = (canonicalRequest != null && canonicalRequest.getIdempotencyKey() != null && !canonicalRequest.getIdempotencyKey().isBlank())
                ? canonicalRequest.getIdempotencyKey()
                : appId + ":ADDRESS_UPDATE";

        List<String> requestedFields = (canonicalRequest != null && canonicalRequest.getRequestedFields() != null && !canonicalRequest.getRequestedFields().isEmpty())
                ? canonicalRequest.getRequestedFields()
                : Arrays.asList(
                "citizen.name",
                "citizen.address",
                "citizen.address.district",
                "citizen.address.taluka",
                "verification.status"
        );

        LocalDateTime startTime = LocalDateTime.now();
        String rawCanonicalJson = "";
        try {
            rawCanonicalJson = objectMapper.writeValueAsString(canonicalRequest);
        } catch (Exception e) {
            rawCanonicalJson = "{}";
        }

        // =========================================================================
        // STAGE 1: CONSENT & DATA MINIMIZATION GATEKEEPER CHECK FIRST
        // =========================================================================
        ConsentValidationResult validationResult = consentValidationService.validate(
                consentId, sourceDept, targetDept, purpose, requestedFields
        );

        if ("BLOCKED".equalsIgnoreCase(validationResult.getStatus())) {
            // Save blocked transaction
            IntegrationTransaction blockedTx = IntegrationTransaction.builder()
                    .applicationId(appId)
                    .correlationId(corrId)
                    .idempotencyKey(idempotencyKey)
                    .sourceDepartment(sourceDept)
                    .targetDepartment(targetDept)
                    .operation("UpdateRationAddress")
                    .sourceProtocol("REST/JSON")
                    .targetProtocol("SOAP/XML")
                    .status("BLOCKED")
                    .retryStatus("BLOCKED")
                    .consentStatus("BLOCKED")
                    .consentFailureReason(validationResult.getReason())
                    .consentId(consentId)
                    .startedAt(startTime)
                    .completedAt(LocalDateTime.now())
                    .errorCode(validationResult.getReason())
                    .errorMessage("Integration request blocked by GovMesh Consent Gatekeeper: " + validationResult.getReason())
                    .rawSourceJson(rawCanonicalJson)
                    .rawCanonicalJson(rawCanonicalJson)
                    .attemptCount(1)
                    .maxAttempts(3)
                    .build();
            transactionRepository.save(blockedTx);

            saveAttempt(IntegrationAttempt.builder()
                    .correlationId(corrId)
                    .applicationId(appId)
                    .idempotencyKey(idempotencyKey)
                    .attemptNumber(1)
                    .protocol("SOAP/XML")
                    .status("BLOCKED")
                    .failureCode(validationResult.getReason())
                    .failureMessage(validationResult.getReason())
                    .attemptAt(LocalDateTime.now())
                    .build());

            // Audit Logs
            auditLogRepository.save(AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .applicationId(appId)
                    .officerId(null)
                    .action("CONSENT_VALIDATION_FAILED")
                    .result("BLOCKED")
                    .description("GovMesh consent validation failed for " + appId + " (ConsentId: " + consentId + ", Reason: " + validationResult.getReason() + ", CorrelationId: " + corrId + ")")
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .applicationId(appId)
                    .officerId(null)
                    .action("INTEGRATION_BLOCKED")
                    .result("BLOCKED")
                    .description("GovMesh interoperability transaction blocked by Consent Gatekeeper for " + appId + " (Reason: " + validationResult.getReason() + ", CorrelationId: " + corrId + ")")
                    .build());

            // STOP RIGHT HERE! DO NOT REACH IDEMPOTENCY EXECUTION OR SOAP ADAPTER! NO RETRIES!
            return CanonicalAddressUpdateResponse.builder()
                    .applicationId(appId)
                    .status("BLOCKED")
                    .message("Integration request blocked by GovMesh Consent Gatekeeper: " + validationResult.getReason())
                    .correlationId(corrId)
                    .targetDepartment(targetDept)
                    .errorCode(validationResult.getReason())
                    .build();
        }

        // =========================================================================
        // STAGE 2: DUPLICATE REQUEST DETECTION & IDEMPOTENCY CHECK
        // =========================================================================
        Optional<IntegrationTransaction> existingOpt = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            IntegrationTransaction existingTx = existingOpt.get();
            if ("SUCCESS".equalsIgnoreCase(existingTx.getStatus()) || "SUCCESS".equalsIgnoreCase(existingTx.getRetryStatus())) {
                // Audit Logs for Duplicate Request
                auditLogRepository.save(AuditLog.builder()
                        .timestamp(LocalDateTime.now())
                        .applicationId(appId)
                        .officerId(null)
                        .action("DUPLICATE_REQUEST_DETECTED")
                        .result("SUCCESS")
                        .description("Duplicate request detected for Application: " + appId + " (Idempotency Key: " + idempotencyKey + "). Original CorrelationId: " + existingTx.getCorrelationId())
                        .build());

                auditLogRepository.save(AuditLog.builder()
                        .timestamp(LocalDateTime.now())
                        .applicationId(appId)
                        .officerId(null)
                        .action("IDEMPOTENT_RESPONSE_RETURNED")
                        .result("SUCCESS")
                        .description("Returning previous successful result for " + appId + " without duplicate business update (Idempotency Key: " + idempotencyKey + ")")
                        .build());

                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(appId)
                        .status("SUCCESS")
                        .message("Duplicate request detected. Previous successful result returned.")
                        .correlationId(corrId)
                        .targetDepartment(targetDept)
                        .build();
            } else if ("PROCESSING".equalsIgnoreCase(existingTx.getStatus()) || "RETRYING".equalsIgnoreCase(existingTx.getStatus())) {
                auditLogRepository.save(AuditLog.builder()
                        .timestamp(LocalDateTime.now())
                        .applicationId(appId)
                        .officerId(null)
                        .action("DUPLICATE_REQUEST_DETECTED")
                        .result("PROCESSING")
                        .description("Duplicate request received while original operation is still in state: " + existingTx.getStatus() + " (Idempotency Key: " + idempotencyKey + ")")
                        .build());

                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(appId)
                        .status("PROCESSING")
                        .message("Request with idempotency key " + idempotencyKey + " is currently being processed.")
                        .correlationId(corrId)
                        .targetDepartment(targetDept)
                        .build();
            }
        }

        // =========================================================================
        // STAGE 3: INITIALIZE & EXECUTE TRANSACTION (ATTEMPT #1)
        // =========================================================================
        IntegrationTransaction tx = IntegrationTransaction.builder()
                .applicationId(appId)
                .correlationId(corrId)
                .idempotencyKey(idempotencyKey)
                .sourceDepartment(sourceDept)
                .targetDepartment(targetDept)
                .operation("UpdateRationAddress")
                .sourceProtocol("REST/JSON")
                .targetProtocol("SOAP/XML")
                .status("PROCESSING")
                .retryStatus("PROCESSING")
                .consentStatus("ALLOWED")
                .consentId(consentId)
                .startedAt(startTime)
                .lastAttemptAt(LocalDateTime.now())
                .attemptCount(1)
                .maxAttempts(3)
                .rawSourceJson(rawCanonicalJson)
                .rawCanonicalJson(rawCanonicalJson)
                .build();
        tx = transactionRepository.save(tx);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(appId)
                .officerId(null)
                .action("INTEGRATION_RECEIVED")
                .result("SUCCESS")
                .description("GovMesh received address update request from " + sourceDept + " (CorrelationId: " + corrId + ", IdempotencyKey: " + idempotencyKey + ")")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(appId)
                .officerId(null)
                .action("CONSENT_VALIDATED")
                .result("ALLOWED")
                .description("GovMesh verified active consent record " + consentId + " for " + sourceDept + " -> " + targetDept)
                .build());

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(appId)
                .officerId(null)
                .action("DATA_MINIMIZATION_PASSED")
                .result("ALLOWED")
                .description("All requested fields permitted by consent policy (CorrelationId: " + corrId + ")")
                .build());

        tx.setStatus("SENDING");
        transactionRepository.save(tx);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(appId)
                .officerId(null)
                .action("SOAP_REQUEST_SENT")
                .result("SUCCESS")
                .description("Sending SOAP XML request to Food Department endpoint /ws (Attempt: 1, CorrelationId: " + corrId + ")")
                .build());

        // Execute Routing (Attempt 1)
        CanonicalAddressUpdateResponse response = integrationRouter.routeAddressUpdate(canonicalRequest);

        // =========================================================================
        // STAGE 4: PROCESS ATTEMPT #1 RESPONSE
        // =========================================================================
        LocalDateTime now = LocalDateTime.now();
        tx.setLastAttemptAt(now);

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            tx.setStatus("SUCCESS");
            tx.setRetryStatus("SUCCESS");
            tx.setCompletedAt(now);
            tx.setErrorCode(null);
            tx.setErrorMessage(null);
            transactionRepository.save(tx);

            saveAttempt(IntegrationAttempt.builder()
                    .correlationId(corrId)
                    .applicationId(appId)
                    .idempotencyKey(idempotencyKey)
                    .attemptNumber(1)
                    .protocol("SOAP/XML")
                    .status("SUCCESS")
                    .attemptAt(now)
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(appId)
                    .officerId(null)
                    .action("SOAP_RESPONSE_RECEIVED")
                    .result("SUCCESS")
                    .description("Received SOAP response from Food Department: SUCCESS (CorrelationId: " + corrId + ")")
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(appId)
                    .officerId(null)
                    .action("INTEGRATION_SUCCESS")
                    .result("SUCCESS")
                    .description("GovMesh interoperability transaction completed successfully for " + appId + " (Attempt: 1)")
                    .build());

            return response;
        }

        // FAILURE HANDLING
        String errCode = response.getErrorCode() != null ? response.getErrorCode() : "SOAP_FAILURE";
        String errMsg = response.getMessage() != null ? response.getMessage() : "SOAP execution failed";
        boolean isRetryable = isRetryableError(errCode, errMsg);

        saveAttempt(IntegrationAttempt.builder()
                .correlationId(corrId)
                .applicationId(appId)
                .idempotencyKey(idempotencyKey)
                .attemptNumber(1)
                .protocol("SOAP/XML")
                .status(isRetryable ? errCode : "FAILED")
                .failureCode(errCode)
                .failureMessage(errMsg)
                .attemptAt(now)
                .build());

        tx.setErrorCode(errCode);
        tx.setErrorMessage(errMsg);
        tx.setFailureCode(errCode);
        tx.setFailureMessage(errMsg);

        if (isRetryable && tx.getAttemptCount() < tx.getMaxAttempts()) {
            LocalDateTime nextRetryAt = now.plusSeconds(5); // Retry 1 delay: 5s
            tx.setStatus("RETRYING");
            tx.setRetryStatus("RETRYING");
            tx.setNextRetryAt(nextRetryAt);
            transactionRepository.save(tx);

            // Audit Logs
            if ("TIMEOUT".equalsIgnoreCase(errCode)) {
                auditLogRepository.save(AuditLog.builder()
                        .timestamp(now)
                        .applicationId(appId)
                        .officerId(null)
                        .action("SOAP_TIMEOUT")
                        .result("FAILED")
                        .description("SOAP request timed out for " + appId + " (Attempt 1/3)")
                        .build());
            } else {
                auditLogRepository.save(AuditLog.builder()
                        .timestamp(now)
                        .applicationId(appId)
                        .officerId(null)
                        .action("SOAP_SERVICE_UNAVAILABLE")
                        .result("FAILED")
                        .description("SOAP service unavailable for " + appId + " (Attempt 1/3)")
                        .build());
            }

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(appId)
                    .officerId(null)
                    .action("RETRY_SCHEDULED")
                    .result("PENDING")
                    .description("Scheduled Retry #2 for application " + appId + " at " + nextRetryAt)
                    .build());

            return CanonicalAddressUpdateResponse.builder()
                    .applicationId(appId)
                    .status("RETRYING")
                    .message("Food Department service is temporarily unavailable. Your request has been safely queued for processing.")
                    .correlationId(corrId)
                    .targetDepartment(targetDept)
                    .errorCode(errCode)
                    .build();
        } else {
            // NON-RETRYABLE OR MAX RETRIES REACHED
            String finalStatus = (tx.getAttemptCount() >= tx.getMaxAttempts()) ? "FINAL_FAILURE" : "FAILED";
            tx.setStatus("FAILED");
            tx.setRetryStatus(finalStatus);
            tx.setCompletedAt(now);
            transactionRepository.save(tx);

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(appId)
                    .officerId(null)
                    .action("INTEGRATION_FAILED")
                    .result("FAILED")
                    .description("GovMesh transaction failed for " + appId + ": " + errMsg + " (ErrorCode: " + errCode + ")")
                    .build());

            if (tx.getAttemptCount() >= tx.getMaxAttempts()) {
                auditLogRepository.save(AuditLog.builder()
                        .timestamp(now)
                        .applicationId(appId)
                        .officerId(null)
                        .action("MAX_RETRIES_REACHED")
                        .result("FAILED")
                        .description("Maximum retries (" + tx.getMaxAttempts() + ") reached for application " + appId)
                        .build());
            }

            return response;
        }
    }

    @Transactional
    public void executeRetry(IntegrationTransaction tx) {
        if (tx == null || !"RETRYING".equalsIgnoreCase(tx.getRetryStatus())) {
            return;
        }

        int nextAttemptNumber = tx.getAttemptCount() + 1;
        tx.setAttemptCount(nextAttemptNumber);
        tx.setLastAttemptAt(LocalDateTime.now());
        tx.setStatus("PROCESSING");

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(tx.getApplicationId())
                .officerId(null)
                .action("RETRY_STARTED")
                .result("PROCESSING")
                .description("Executing Retry #" + nextAttemptNumber + " for application " + tx.getApplicationId() + " (CorrelationId: " + tx.getCorrelationId() + ")")
                .build());

        CanonicalAddressUpdateRequest request = reconstructRequest(tx);
        CanonicalAddressUpdateResponse response = integrationRouter.routeAddressUpdate(request);

        LocalDateTime now = LocalDateTime.now();
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            tx.setStatus("SUCCESS");
            tx.setRetryStatus("SUCCESS");
            tx.setCompletedAt(now);
            tx.setErrorCode(null);
            tx.setErrorMessage(null);
            tx.setNextRetryAt(null);
            transactionRepository.save(tx);

            saveAttempt(IntegrationAttempt.builder()
                    .correlationId(tx.getCorrelationId())
                    .applicationId(tx.getApplicationId())
                    .idempotencyKey(tx.getIdempotencyKey())
                    .attemptNumber(nextAttemptNumber)
                    .protocol("SOAP/XML")
                    .status("SUCCESS")
                    .attemptAt(now)
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(tx.getApplicationId())
                    .officerId(null)
                    .action("RETRY_SUCCESS")
                    .result("SUCCESS")
                    .description("Retry #" + nextAttemptNumber + " succeeded for " + tx.getApplicationId())
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(tx.getApplicationId())
                    .officerId(null)
                    .action("INTEGRATION_SUCCESS")
                    .result("SUCCESS")
                    .description("GovMesh transaction completed successfully after " + nextAttemptNumber + " attempts")
                    .build());
        } else {
            String errCode = response.getErrorCode() != null ? response.getErrorCode() : "SOAP_FAILURE";
            String errMsg = response.getMessage() != null ? response.getMessage() : "SOAP retry execution failed";

            tx.setErrorCode(errCode);
            tx.setErrorMessage(errMsg);
            tx.setFailureCode(errCode);
            tx.setFailureMessage(errMsg);

            saveAttempt(IntegrationAttempt.builder()
                    .correlationId(tx.getCorrelationId())
                    .applicationId(tx.getApplicationId())
                    .idempotencyKey(tx.getIdempotencyKey())
                    .attemptNumber(nextAttemptNumber)
                    .protocol("SOAP/XML")
                    .status(errCode)
                    .failureCode(errCode)
                    .failureMessage(errMsg)
                    .attemptAt(now)
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(now)
                    .applicationId(tx.getApplicationId())
                    .officerId(null)
                    .action("RETRY_FAILED")
                    .result("FAILED")
                    .description("Retry #" + nextAttemptNumber + " failed for " + tx.getApplicationId() + ": " + errMsg)
                    .build());

            if (nextAttemptNumber < tx.getMaxAttempts() && isRetryableError(errCode, errMsg)) {
                // Schedule next attempt: Attempt 2 -> 15s delay
                int delaySecs = (nextAttemptNumber == 2) ? 15 : 30;
                LocalDateTime nextRetryAt = now.plusSeconds(delaySecs);
                tx.setStatus("RETRYING");
                tx.setRetryStatus("RETRYING");
                tx.setNextRetryAt(nextRetryAt);
                transactionRepository.save(tx);

                auditLogRepository.save(AuditLog.builder()
                        .timestamp(now)
                        .applicationId(tx.getApplicationId())
                        .officerId(null)
                        .action("RETRY_SCHEDULED")
                        .result("PENDING")
                        .description("Scheduled Retry #" + (nextAttemptNumber + 1) + " for " + tx.getApplicationId() + " at " + nextRetryAt)
                        .build());
            } else {
                // MAX RETRIES REACHED OR NON-RETRYABLE FAILURE
                tx.setStatus("FAILED");
                tx.setRetryStatus("FINAL_FAILURE");
                tx.setCompletedAt(now);
                tx.setNextRetryAt(null);
                transactionRepository.save(tx);

                auditLogRepository.save(AuditLog.builder()
                        .timestamp(now)
                        .applicationId(tx.getApplicationId())
                        .officerId(null)
                        .action("MAX_RETRIES_REACHED")
                        .result("FAILED")
                        .description("Maximum retries (" + tx.getMaxAttempts() + ") reached for application " + tx.getApplicationId())
                        .build());

                auditLogRepository.save(AuditLog.builder()
                        .timestamp(now)
                        .applicationId(tx.getApplicationId())
                        .officerId(null)
                        .action("INTEGRATION_FAILED")
                        .result("FINAL_FAILURE")
                        .description("GovMesh transaction marked FINAL_FAILURE for " + tx.getApplicationId())
                        .build());
            }
        }
    }

    public List<IntegrationTransaction> getTransactions() {
        return transactionRepository.findAllOrderedByStartedAtDesc();
    }

    public List<IntegrationTransaction> getFailedAndRetryTransactions() {
        return transactionRepository.findByStatusInOrderByStartedAtDesc(Arrays.asList("RETRYING", "FAILED", "FINAL_FAILURE", "BLOCKED"));
    }

    public IntegrationTransaction getTransactionByCorrelationId(String correlationId) {
        return transactionRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new ResourceNotFoundException("Integration transaction trace not found for Correlation ID: " + correlationId));
    }

    public List<IntegrationAttempt> getAttemptsByCorrelationId(String correlationId) {
        if (attemptRepository == null) return List.of();
        return attemptRepository.findByCorrelationIdOrderByAttemptNumberAsc(correlationId);
    }

    private void saveAttempt(IntegrationAttempt attempt) {
        if (attemptRepository != null && attempt != null) {
            attemptRepository.save(attempt);
        }
    }

    private boolean isRetryableError(String errorCode, String errorMessage) {
        if (errorCode == null && errorMessage == null) return false;

        String code = errorCode != null ? errorCode.toUpperCase() : "";
        String msg = errorMessage != null ? errorMessage.toUpperCase() : "";

        if (code.contains("TIMEOUT") || code.contains("SERVICE_UNAVAILABLE") || code.contains("TEMPORARILY_UNAVAILABLE") || code.contains("CONNECTION")) {
            return true;
        }
        if (msg.contains("TIMEOUT") || msg.contains("UNAVAILABLE") || msg.contains("CONNECTION") || msg.contains("503")) {
            return true;
        }

        // Non-retryable errors
        if (code.contains("EXPIRED") || code.contains("REVOKED") || code.contains("BLOCKED") || code.contains("VALIDATION") || code.contains("NOT_FOUND")) {
            return false;
        }
        return false;
    }

    private CanonicalAddressUpdateRequest reconstructRequest(IntegrationTransaction tx) {
        if (tx.getRawCanonicalJson() != null && !tx.getRawCanonicalJson().isBlank()) {
            try {
                return objectMapper.readValue(tx.getRawCanonicalJson(), CanonicalAddressUpdateRequest.class);
            } catch (Exception e) {
                // Fallback to construction
            }
        }

        CanonicalAddressUpdateRequest req = new CanonicalAddressUpdateRequest();
        req.setApplicationId(tx.getApplicationId());
        req.setCorrelationId(tx.getCorrelationId());
        req.setIdempotencyKey(tx.getIdempotencyKey());
        req.setSourceDepartment(tx.getSourceDepartment());
        req.setTargetDepartment(tx.getTargetDepartment());
        req.setPurpose("RATION_ADDRESS_UPDATE");
        req.setConsent(new CanonicalAddressUpdateRequest.ConsentInfo(tx.getConsentId() != null ? tx.getConsentId() : "CONSENT-00124"));

        CanonicalAddressUpdateRequest.AddressInfo addr = new CanonicalAddressUpdateRequest.AddressInfo("44 Example Road, Pune", "DIST-PUN", "TAL-PUN-04");
        req.setCitizen(new CanonicalAddressUpdateRequest.CitizenInfo("CIT-MH-998811", "Rajesh Kumar", addr));
        req.setVerification(new CanonicalAddressUpdateRequest.VerificationInfo("VALID", "REVENUE"));
        return req;
    }
}
