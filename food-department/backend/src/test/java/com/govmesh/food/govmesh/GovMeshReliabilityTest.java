package com.govmesh.food.govmesh;

import com.govmesh.food.entity.Application;
import com.govmesh.food.entity.AuditLog;
import com.govmesh.food.entity.Consent;
import com.govmesh.food.entity.IntegrationAttempt;
import com.govmesh.food.entity.IntegrationTransaction;
import com.govmesh.food.entity.RationRecord;
import com.govmesh.food.govmesh.adapter.FoodDepartmentAdapter;
import com.govmesh.food.govmesh.config.SoapSimulationConfig;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateRequest;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateResponse;
import com.govmesh.food.govmesh.mapper.FoodDepartmentSchemaMapper;
import com.govmesh.food.govmesh.router.IntegrationRouter;
import com.govmesh.food.govmesh.service.ConsentValidationService;
import com.govmesh.food.govmesh.service.GovMeshInteroperabilityService;
import com.govmesh.food.repository.ApplicationRepository;
import com.govmesh.food.repository.AuditLogRepository;
import com.govmesh.food.repository.ConsentRepository;
import com.govmesh.food.repository.IntegrationAttemptRepository;
import com.govmesh.food.repository.IntegrationTransactionRepository;
import com.govmesh.food.repository.RationRecordRepository;
import com.govmesh.food.service.ApplicationService;
import com.govmesh.food.service.ConsentPolicyService;
import com.govmesh.food.soap.dto.UpdateRationAddress;
import com.govmesh.food.soap.endpoint.FoodDepartmentSoapEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GovMeshReliabilityTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RationRecordRepository rationRecordRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private IntegrationTransactionRepository transactionRepository;

    @Mock
    private IntegrationAttemptRepository attemptRepository;

    @Mock
    private ConsentRepository consentRepository;

    private ConsentPolicyService consentPolicyService;
    private ConsentValidationService consentValidationService;
    private FoodDepartmentSchemaMapper schemaMapper;
    private ApplicationService applicationService;
    private FoodDepartmentSoapEndpoint soapEndpoint;
    private SoapSimulationConfig simulationConfig;
    private FoodDepartmentAdapter foodAdapter;
    private IntegrationRouter integrationRouter;
    private GovMeshInteroperabilityService interoperabilityService;

    private Application sampleApp;
    private RationRecord sampleRecord;
    private Consent activeConsent;
    private Consent expiredConsent;

    private final Map<String, IntegrationTransaction> txDatabase = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        txDatabase.clear();

        consentPolicyService = new ConsentPolicyService();
        consentValidationService = new ConsentValidationService(consentRepository, consentPolicyService);
        schemaMapper = new FoodDepartmentSchemaMapper(applicationRepository);
        applicationService = new ApplicationService(applicationRepository, rationRecordRepository, auditLogRepository, mock());
        soapEndpoint = new FoodDepartmentSoapEndpoint(applicationService);

        simulationConfig = new SoapSimulationConfig("SUCCESS");

        foodAdapter = new FoodDepartmentAdapter(schemaMapper, simulationConfig) {
            @Override
            public CanonicalAddressUpdateResponse sendAddressUpdate(CanonicalAddressUpdateRequest canonicalRequest, String soapEndpointUrl) {
                // Respect simulation config mode
                if (simulationConfig.getMode() == SoapSimulationConfig.SimulationMode.TIMEOUT) {
                    return CanonicalAddressUpdateResponse.builder()
                            .applicationId(canonicalRequest.getApplicationId())
                            .status("FAILED")
                            .message("Food Department SOAP service timed out while waiting for response.")
                            .correlationId(canonicalRequest.getCorrelationId())
                            .targetDepartment("FOOD")
                            .errorCode("TIMEOUT")
                            .build();
                } else if (simulationConfig.getMode() == SoapSimulationConfig.SimulationMode.SERVICE_UNAVAILABLE) {
                    return CanonicalAddressUpdateResponse.builder()
                            .applicationId(canonicalRequest.getApplicationId())
                            .status("FAILED")
                            .message("Food Department SOAP service is temporarily unavailable.")
                            .correlationId(canonicalRequest.getCorrelationId())
                            .targetDepartment("FOOD")
                            .errorCode("SERVICE_UNAVAILABLE")
                            .build();
                }

                UpdateRationAddress soapReq = schemaMapper.mapCanonicalToSoapRequest(canonicalRequest);
                try {
                    var soapResp = soapEndpoint.updateRationAddress(soapReq);
                    return CanonicalAddressUpdateResponse.builder()
                            .applicationId(soapResp.getApplicationId())
                            .status(soapResp.getStatus())
                            .message(soapResp.getMessage())
                            .correlationId(soapResp.getCorrelationId())
                            .targetDepartment("FOOD")
                            .build();
                } catch (Exception ex) {
                    return CanonicalAddressUpdateResponse.builder()
                            .applicationId(canonicalRequest.getApplicationId())
                            .status("FAILED")
                            .message(ex.getMessage())
                            .correlationId(canonicalRequest.getCorrelationId())
                            .targetDepartment("FOOD")
                            .errorCode("SOAP_FAULT")
                            .build();
                }
            }
        };

        integrationRouter = new IntegrationRouter(foodAdapter, "http://localhost:8080/ws");
        interoperabilityService = new GovMeshInteroperabilityService(integrationRouter, transactionRepository, attemptRepository, auditLogRepository, consentValidationService);

        sampleRecord = RationRecord.builder()
                .id(1L)
                .rationCardNo("MH12-2026-000124")
                .holderName("Rajesh Kumar")
                .houseAddress("12, M.G. Road, Shivajinagar, Pune")
                .districtCode("DIST-PUN")
                .talukaCode("TAL-PUN-04")
                .verificationFlag(true)
                .updateStatus("ACTIVE")
                .build();

        sampleApp = Application.builder()
                .id(1L)
                .applicationId("GM-2026-000124")
                .citizenReference("CIT-MH-998811")
                .rationCardNo("MH12-2026-000124")
                .applicationType("ADDRESS_UPDATE")
                .currentStatus("PENDING")
                .sourceDepartment("REVENUE")
                .requestedAddress("44 Example Road, Shivajinagar, Pune - 411005")
                .build();

        activeConsent = Consent.builder()
                .id(1L)
                .consentId("CONSENT-00124")
                .citizenReference("CIT-MH-998811")
                .requestingDepartment("REVENUE")
                .receivingDepartment("FOOD")
                .purpose("RATION_ADDRESS_UPDATE")
                .status("ACTIVE")
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        expiredConsent = Consent.builder()
                .id(2L)
                .consentId("CONSENT-EXPIRED-001")
                .citizenReference("CIT-MH-998811")
                .requestingDepartment("REVENUE")
                .receivingDepartment("FOOD")
                .purpose("RATION_ADDRESS_UPDATE")
                .status("EXPIRED")
                .issuedAt(LocalDateTime.now().minusDays(30))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(consentRepository.findByConsentId("CONSENT-00124")).thenReturn(Optional.of(activeConsent));
        when(consentRepository.findByConsentId("CONSENT-EXPIRED-001")).thenReturn(Optional.of(expiredConsent));

        when(applicationRepository.findByApplicationId("GM-2026-000124")).thenReturn(Optional.of(sampleApp));
        when(rationRecordRepository.findByRationCardNo("MH12-2026-000124")).thenReturn(Optional.of(sampleRecord));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(rationRecordRepository.save(any(RationRecord.class))).thenAnswer(i -> i.getArgument(0));

        when(transactionRepository.save(any(IntegrationTransaction.class))).thenAnswer(i -> {
            IntegrationTransaction t = i.getArgument(0);
            if (t.getId() == null) t.setId(System.currentTimeMillis());
            if (t.getIdempotencyKey() != null) {
                txDatabase.put(t.getIdempotencyKey(), t);
            }
            return t;
        });

        when(transactionRepository.findByIdempotencyKey(anyString())).thenAnswer(i -> {
            String key = i.getArgument(0);
            return Optional.ofNullable(txDatabase.get(key));
        });
    }

    // TEST 1 — NORMAL SUCCESS
    @Test
    void test1_NormalSuccess() {
        CanonicalAddressUpdateRequest request = createSampleRequest("REQ-2026-000124", "CONSENT-00124");
        CanonicalAddressUpdateResponse response = interoperabilityService.processInteroperabilityRequest(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("GM-2026-000124", response.getApplicationId());
        assertEquals("APPROVED", sampleApp.getCurrentStatus());
        assertEquals("44 Example Road, Shivajinagar, Pune - 411005", sampleRecord.getHouseAddress());

        verify(attemptRepository, times(1)).save(any(IntegrationAttempt.class));
        verify(auditLogRepository, atLeast(4)).save(any(AuditLog.class));
    }

    // TEST 2 — DUPLICATE REQUEST
    @Test
    void test2_DuplicateRequest() {
        CanonicalAddressUpdateRequest req1 = createSampleRequest("REQ-2026-000124", "CONSENT-00124");
        CanonicalAddressUpdateResponse resp1 = interoperabilityService.processInteroperabilityRequest(req1);
        assertEquals("SUCCESS", resp1.getStatus());

        // Reset invocation counter on ration record save to verify no 2nd update
        clearInvocations(rationRecordRepository);

        CanonicalAddressUpdateRequest req2 = createSampleRequest("REQ-2026-000124", "CONSENT-00124");
        CanonicalAddressUpdateResponse resp2 = interoperabilityService.processInteroperabilityRequest(req2);

        assertNotNull(resp2);
        assertEquals("SUCCESS", resp2.getStatus());
        assertTrue(resp2.getMessage().contains("Duplicate request detected"));

        // Verify NO second database update performed on RationRecord!
        verify(rationRecordRepository, never()).save(any(RationRecord.class));
        verify(auditLogRepository, atLeast(1)).save(argThat(log -> "DUPLICATE_REQUEST_DETECTED".equals(log.getAction())));
    }

    // TEST 3 — SOAP TIMEOUT
    @Test
    void test3_SoapTimeout_AndScheduledRetrySuccess() {
        simulationConfig.setMode("TIMEOUT");

        CanonicalAddressUpdateRequest request = createSampleRequest("REQ-2026-TIMEOUT-1", "CONSENT-00124");
        CanonicalAddressUpdateResponse response = interoperabilityService.processInteroperabilityRequest(request);

        assertNotNull(response);
        assertEquals("RETRYING", response.getStatus());
        assertEquals("TIMEOUT", response.getErrorCode());

        IntegrationTransaction tx = txDatabase.get("GM-2026-000124:ADDRESS_UPDATE");
        assertNotNull(tx);
        assertEquals("RETRYING", tx.getRetryStatus());
        assertEquals(1, tx.getAttemptCount());
        assertNotNull(tx.getNextRetryAt());

        // Now fix the service (back to SUCCESS) and execute retry #2
        simulationConfig.setMode("SUCCESS");
        interoperabilityService.executeRetry(tx);

        assertEquals("SUCCESS", tx.getRetryStatus());
        assertEquals(2, tx.getAttemptCount());
        assertEquals("APPROVED", sampleApp.getCurrentStatus());
    }

    // TEST 4 — SERVICE UNAVAILABLE
    @Test
    void test4_ServiceUnavailable_TriggersRetry() {
        simulationConfig.setMode("SERVICE_UNAVAILABLE");

        CanonicalAddressUpdateRequest request = createSampleRequest("REQ-2026-UNAVAIL-1", "CONSENT-00124");
        CanonicalAddressUpdateResponse response = interoperabilityService.processInteroperabilityRequest(request);

        assertEquals("RETRYING", response.getStatus());
        assertEquals("SERVICE_UNAVAILABLE", response.getErrorCode());

        IntegrationTransaction tx = txDatabase.get("GM-2026-000124:ADDRESS_UPDATE");
        assertEquals("RETRYING", tx.getRetryStatus());
        assertEquals(1, tx.getAttemptCount());
    }

    // TEST 5 — MAX RETRIES
    @Test
    void test5_MaxRetriesReached_MarksFinalFailure() {
        simulationConfig.setMode("SERVICE_UNAVAILABLE");

        CanonicalAddressUpdateRequest request = createSampleRequest("REQ-2026-MAXRETRY", "CONSENT-00124");
        interoperabilityService.processInteroperabilityRequest(request);

        IntegrationTransaction tx = txDatabase.get("GM-2026-000124:ADDRESS_UPDATE");
        assertEquals(1, tx.getAttemptCount());

        // Attempt 2
        interoperabilityService.executeRetry(tx);
        assertEquals(2, tx.getAttemptCount());
        assertEquals("RETRYING", tx.getRetryStatus());

        // Attempt 3 (Max)
        interoperabilityService.executeRetry(tx);
        assertEquals(3, tx.getAttemptCount());
        assertEquals("FINAL_FAILURE", tx.getRetryStatus());
        assertEquals("FAILED", tx.getStatus());

        verify(auditLogRepository, atLeast(1)).save(argThat(log -> "MAX_RETRIES_REACHED".equals(log.getAction())));
    }

    // TEST 6 — NON-RETRYABLE ERROR
    @Test
    void test6_NonRetryableError_ExpiredConsent_DoesNotRetry() {
        CanonicalAddressUpdateRequest request = createSampleRequest("REQ-2026-EXPIRED", "CONSENT-EXPIRED-001");
        CanonicalAddressUpdateResponse response = interoperabilityService.processInteroperabilityRequest(request);

        assertEquals("BLOCKED", response.getStatus());
        assertEquals("CONSENT_EXPIRED", response.getErrorCode());

        IntegrationTransaction tx = txDatabase.get("GM-2026-000124:ADDRESS_UPDATE");
        assertNotNull(tx);
        assertEquals("BLOCKED", tx.getRetryStatus());
        assertNull(tx.getNextRetryAt());

        // Verify SOAP endpoint was NEVER called!
        verify(rationRecordRepository, never()).save(any(RationRecord.class));
    }

    // TEST 7 — IDEMPOTENCY AFTER RETRY (FOOD ENDPOINT PROTECTION)
    @Test
    void test7_FoodEndpointIdempotencyProtection_PreventsDuplicateMutations() {
        // Step 1: Execute initial success
        CanonicalAddressUpdateRequest req1 = createSampleRequest("REQ-2026-DUP-SOAP", "CONSENT-00124");
        interoperabilityService.processInteroperabilityRequest(req1);

        assertEquals("APPROVED", sampleApp.getCurrentStatus());
        assertEquals("44 Example Road, Shivajinagar, Pune - 411005", sampleRecord.getHouseAddress());

        // Reset mocks to count subsequent updates
        clearInvocations(rationRecordRepository);

        // Step 2: Simulate retry hitting Food SOAP endpoint again directly
        UpdateRationAddress soapCommand = schemaMapper.mapCanonicalToSoapRequest(req1);
        var soapResponse = soapEndpoint.updateRationAddress(soapCommand);

        assertNotNull(soapResponse);
        assertEquals("SUCCESS", soapResponse.getStatus());
        assertTrue(soapResponse.getMessage().contains("already processed"));

        // Verify Food department did NOT update RationRecord a second time!
        verify(rationRecordRepository, never()).save(any(RationRecord.class));
        verify(auditLogRepository, atLeast(1)).save(argThat(log -> "DUPLICATE_SOAP_REQUEST_DETECTED".equals(log.getAction())));
    }

    private CanonicalAddressUpdateRequest createSampleRequest(String corrId, String consentId) {
        CanonicalAddressUpdateRequest req = new CanonicalAddressUpdateRequest(
                "GM-2026-000124",
                "REVENUE",
                "FOOD",
                corrId,
                new CanonicalAddressUpdateRequest.CitizenInfo("CIT-MH-998811", "Rajesh Kumar",
                        new CanonicalAddressUpdateRequest.AddressInfo("44 Example Road, Shivajinagar, Pune - 411005", "DIST-PUN", "TAL-PUN-04")),
                new CanonicalAddressUpdateRequest.VerificationInfo("VALID", "REVENUE"),
                new CanonicalAddressUpdateRequest.ConsentInfo(consentId)
        );
        req.setIdempotencyKey("GM-2026-000124:ADDRESS_UPDATE");
        return req;
    }
}
