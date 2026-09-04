package com.govmesh.food.govmesh;

import com.govmesh.food.entity.AuditLog;
import com.govmesh.food.entity.Consent;
import com.govmesh.food.entity.IntegrationAttempt;
import com.govmesh.food.entity.IntegrationTransaction;
import com.govmesh.food.exception.ResourceNotFoundException;
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
import com.govmesh.food.service.ConsentPolicyService;
import com.govmesh.food.service.SystemHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class GovMeshPostgresPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IntegrationTransactionRepository transactionRepository;

    @Autowired
    private IntegrationAttemptRepository attemptRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private DataSource dataSource;

    private GovMeshInteroperabilityService interoperabilityService;
    private ConsentValidationService consentValidationService;
    private SystemHealthService systemHealthService;

    @BeforeEach
    void setUp() {
        ConsentPolicyService consentPolicyService = new ConsentPolicyService();
        consentValidationService = new ConsentValidationService(consentRepository, consentPolicyService);

        SoapSimulationConfig simulationConfig = new SoapSimulationConfig("SUCCESS");
        FoodDepartmentSchemaMapper schemaMapper = new FoodDepartmentSchemaMapper(applicationRepository);

        FoodDepartmentAdapter foodAdapter = new FoodDepartmentAdapter(schemaMapper, simulationConfig) {
            @Override
            public CanonicalAddressUpdateResponse sendAddressUpdate(CanonicalAddressUpdateRequest canonicalRequest, String soapEndpointUrl) {
                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(canonicalRequest.getApplicationId())
                        .status("SUCCESS")
                        .message("Ration address update processed and persisted successfully")
                        .correlationId(canonicalRequest.getCorrelationId())
                        .targetDepartment("FOOD")
                        .build();
            }
        };

        IntegrationRouter integrationRouter = new IntegrationRouter(foodAdapter, "http://localhost:8081/ws");

        interoperabilityService = new GovMeshInteroperabilityService(
                integrationRouter,
                transactionRepository,
                attemptRepository,
                auditLogRepository,
                consentValidationService
        );

        systemHealthService = new SystemHealthService(dataSource);

        // Seed default active consent for tests
        consentRepository.save(Consent.builder()
                .consentId("CONSENT-00124")
                .citizenReference("CIT-MH-998811")
                .requestingDepartment("REVENUE")
                .receivingDepartment("FOOD")
                .purpose("RATION_ADDRESS_UPDATE")
                .status("ACTIVE")
                .issuedAt(LocalDateTime.now().minusDays(5))
                .expiresAt(LocalDateTime.now().plusDays(25))
                .build());
    }

    @Test
    @DisplayName("1. Create and Persist Transaction in Database")
    void testCreateAndRetrieveTransaction() {
        IntegrationTransaction tx = IntegrationTransaction.builder()
                .applicationId("GM-2026-TEST-01")
                .correlationId("REQ-TEST-CORR-01")
                .idempotencyKey("KEY-TEST-01")
                .sourceDepartment("REVENUE")
                .targetDepartment("FOOD")
                .operation("UpdateRationAddress")
                .sourceProtocol("REST/JSON")
                .targetProtocol("SOAP/XML")
                .status("PROCESSING")
                .retryStatus("PROCESSING")
                .startedAt(LocalDateTime.now())
                .attemptCount(1)
                .maxAttempts(3)
                .build();

        IntegrationTransaction savedTx = transactionRepository.save(tx);
        assertNotNull(savedTx.getId(), "Saved transaction must have a database primary key");

        Optional<IntegrationTransaction> foundOpt = transactionRepository.findByCorrelationId("REQ-TEST-CORR-01");
        assertTrue(foundOpt.isPresent());
        assertEquals("GM-2026-TEST-01", foundOpt.get().getApplicationId());
        assertEquals("PROCESSING", foundOpt.get().getStatus());
    }

    @Test
    @DisplayName("2. Update Transaction Status and Progress")
    void testUpdateTransactionStatusAndProgress() {
        IntegrationTransaction tx = IntegrationTransaction.builder()
                .applicationId("GM-2026-TEST-02")
                .correlationId("REQ-TEST-CORR-02")
                .idempotencyKey("KEY-TEST-02")
                .sourceDepartment("REVENUE")
                .targetDepartment("FOOD")
                .operation("UpdateRationAddress")
                .sourceProtocol("REST/JSON")
                .targetProtocol("SOAP/XML")
                .status("PROCESSING")
                .retryStatus("PROCESSING")
                .startedAt(LocalDateTime.now())
                .attemptCount(1)
                .maxAttempts(3)
                .build();

        tx = transactionRepository.save(tx);

        // Update status to SENDING then SUCCESS
        tx.setStatus("SENDING");
        transactionRepository.save(tx);
        assertEquals("SENDING", transactionRepository.findByCorrelationId("REQ-TEST-CORR-02").get().getStatus());

        tx.setStatus("SUCCESS");
        tx.setRetryStatus("SUCCESS");
        tx.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        IntegrationTransaction updatedTx = transactionRepository.findByCorrelationId("REQ-TEST-CORR-02").get();
        assertEquals("SUCCESS", updatedTx.getStatus());
        assertEquals("SUCCESS", updatedTx.getRetryStatus());
        assertNotNull(updatedTx.getCompletedAt());
    }

    @Test
    @DisplayName("3. Persist Department Workflow Execution State")
    void testPersistDepartmentExecutionState() {
        CanonicalAddressUpdateRequest request = createSampleRequest("GM-2026-DEPT-01", "CORR-DEPT-01", "IDEMP-DEPT-01");

        CanonicalAddressUpdateResponse response = interoperabilityService.processInteroperabilityRequest(request);
        assertEquals("SUCCESS", response.getStatus());

        IntegrationTransaction tx = transactionRepository.findByCorrelationId("CORR-DEPT-01").orElse(null);
        assertNotNull(tx);
        assertEquals("REVENUE", tx.getSourceDepartment());
        assertEquals("FOOD", tx.getTargetDepartment());
        assertEquals("REST/JSON", tx.getSourceProtocol());
        assertEquals("SOAP/XML", tx.getTargetProtocol());
        assertEquals(1, tx.getAttemptCount());
        assertNotNull(tx.getLastAttemptAt());

        List<IntegrationAttempt> attempts = attemptRepository.findByCorrelationIdOrderByAttemptNumberAsc("CORR-DEPT-01");
        assertFalse(attempts.isEmpty());
        assertEquals(1, attempts.get(0).getAttemptNumber());
        assertEquals("SOAP/XML", attempts.get(0).getProtocol());
        assertEquals("SUCCESS", attempts.get(0).getStatus());
    }

    @Test
    @DisplayName("4. Persist Consent Record")
    void testPersistConsentRecord() {
        Consent consent = Consent.builder()
                .consentId("CONSENT-TEST-99")
                .citizenReference("CIT-MH-112233")
                .requestingDepartment("REVENUE")
                .receivingDepartment("FOOD")
                .purpose("RATION_ADDRESS_UPDATE")
                .status("ACTIVE")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        Consent saved = consentRepository.save(consent);
        assertNotNull(saved.getId());

        Optional<Consent> foundOpt = consentRepository.findByConsentId("CONSENT-TEST-99");
        assertTrue(foundOpt.isPresent());
        assertEquals("CIT-MH-112233", foundOpt.get().getCitizenReference());
    }

    @Test
    @DisplayName("5. Persist Append-Oriented Audit Logs")
    void testPersistAuditEvent() {
        long initialCount = auditLogRepository.count();

        AuditLog logEntry = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId("GM-2026-AUDIT-01")
                .officerId(101L)
                .action("DATABASE_PERSISTENCE_CHECK")
                .result("SUCCESS")
                .description("Testing persistent audit log event write")
                .build();

        auditLogRepository.save(logEntry);

        assertEquals(initialCount + 1, auditLogRepository.count());
        List<AuditLog> logs = auditLogRepository.findByApplicationIdOrderByTimestampDesc("GM-2026-AUDIT-01");
        assertFalse(logs.isEmpty());
        assertEquals("DATABASE_PERSISTENCE_CHECK", logs.get(0).getAction());
    }

    @Test
    @DisplayName("6. Transaction State Survives Backend Restart Simulation")
    void testTransactionSurvivesRestartSimulation() {
        // Step 1: Create transaction in persistence layer
        CanonicalAddressUpdateRequest request = createSampleRequest("GM-2026-RESTART-01", "CORR-RESTART-01", "IDEMP-RESTART-01");
        interoperabilityService.processInteroperabilityRequest(request);

        // Flush and clear EntityManager to simulate process restart / memory wipe
        entityManager.flush();
        entityManager.clear();

        // Step 2: Create brand new service instance (simulating backend application restart)
        SoapSimulationConfig simulationConfig = new SoapSimulationConfig("SUCCESS");
        FoodDepartmentSchemaMapper schemaMapper = new FoodDepartmentSchemaMapper(applicationRepository);

        FoodDepartmentAdapter foodAdapter = new FoodDepartmentAdapter(schemaMapper, simulationConfig) {
            @Override
            public CanonicalAddressUpdateResponse sendAddressUpdate(CanonicalAddressUpdateRequest canonicalRequest, String soapEndpointUrl) {
                return CanonicalAddressUpdateResponse.builder()
                        .applicationId(canonicalRequest.getApplicationId())
                        .status("SUCCESS")
                        .message("Ration address update processed and persisted successfully")
                        .correlationId(canonicalRequest.getCorrelationId())
                        .targetDepartment("FOOD")
                        .build();
            }
        };
        IntegrationRouter integrationRouter = new IntegrationRouter(foodAdapter, "http://localhost:8081/ws");

        GovMeshInteroperabilityService newInteroperabilityService = new GovMeshInteroperabilityService(
                integrationRouter,
                transactionRepository,
                attemptRepository,
                auditLogRepository,
                consentValidationService
        );

        // Step 3: Fetch transaction from newly instantiated service
        IntegrationTransaction retrievedTx = newInteroperabilityService.getTransactionByCorrelationId("CORR-RESTART-01");
        assertNotNull(retrievedTx, "Transaction must survive backend restart");
        assertEquals("GM-2026-RESTART-01", retrievedTx.getApplicationId());
        assertEquals("SUCCESS", retrievedTx.getStatus());
        assertEquals("IDEMP-RESTART-01", retrievedTx.getIdempotencyKey());
    }

    @Test
    @DisplayName("7. Non-existent Transaction Throws Expected ResourceNotFoundException (404)")
    void testMissingTransactionHandling() {
        assertThrows(ResourceNotFoundException.class, () -> {
            interoperabilityService.getTransactionByCorrelationId("NON-EXISTENT-CORRELATION-ID");
        });
    }

    @Test
    @DisplayName("8. Duplicate Transaction Idempotency Persistence Check")
    void testDuplicateTransactionIdempotency() {
        CanonicalAddressUpdateRequest req1 = createSampleRequest("GM-2026-IDEMP-01", "CORR-IDEMP-01", "KEY-IDEMP-99");
        CanonicalAddressUpdateResponse res1 = interoperabilityService.processInteroperabilityRequest(req1);
        assertEquals("SUCCESS", res1.getStatus());

        // Process duplicate request with SAME idempotency key
        CanonicalAddressUpdateRequest req2 = createSampleRequest("GM-2026-IDEMP-01", "CORR-IDEMP-02", "KEY-IDEMP-99");
        CanonicalAddressUpdateResponse res2 = interoperabilityService.processInteroperabilityRequest(req2);

        assertEquals("SUCCESS", res2.getStatus());
        assertTrue(res2.getMessage().contains("Duplicate request detected"));

        // Confirm only 1 transaction record exists in DB for this idempotency key
        Optional<IntegrationTransaction> txOpt = transactionRepository.findByIdempotencyKey("KEY-IDEMP-99");
        assertTrue(txOpt.isPresent());
        assertEquals("CORR-IDEMP-01", txOpt.get().getCorrelationId(), "Original correlation ID must be preserved");
    }

    @Test
    @DisplayName("9. Database Health Check Returns OPERATIONAL")
    void testDatabaseConnectivityHealthCheck() {
        com.govmesh.food.dto.SystemHealthDTO health = systemHealthService.getSystemHealth();
        assertNotNull(health);
        assertNotNull(health.getComponents());

        com.govmesh.food.dto.SystemHealthDTO.ComponentHealth dbComponent = health.getComponents().stream()
                .filter(c -> c.getName().contains("PostgreSQL Database Service"))
                .findFirst()
                .orElse(null);

        assertNotNull(dbComponent);
        assertEquals("OPERATIONAL", dbComponent.getStatus());
        assertTrue(dbComponent.getMessage().contains("healthy"));
    }

    @Test
    @DisplayName("10. Database Health Check Handles Connectivity Failures Gracefully")
    void testDatabaseUnavailableHandling() {
        DataSource mockDataSource = new DriverManagerDataSource("jdbc:invalid:host:9999/dummy", "user", "pass");
        SystemHealthService failingHealthService = new SystemHealthService(mockDataSource);

        com.govmesh.food.dto.SystemHealthDTO health = failingHealthService.getSystemHealth();
        com.govmesh.food.dto.SystemHealthDTO.ComponentHealth dbComponent = health.getComponents().stream()
                .filter(c -> c.getName().contains("PostgreSQL Database Service"))
                .findFirst()
                .orElse(null);

        assertNotNull(dbComponent);
        assertEquals("DOWN", dbComponent.getStatus());
        assertTrue(dbComponent.getMessage().contains("failed"));
        assertFalse(dbComponent.getMessage().contains("pass="), "Passwords must never be exposed in error messages");
    }

    private CanonicalAddressUpdateRequest createSampleRequest(String appId, String corrId, String idempotencyKey) {
        CanonicalAddressUpdateRequest req = new CanonicalAddressUpdateRequest();
        req.setApplicationId(appId);
        req.setCorrelationId(corrId);
        req.setIdempotencyKey(idempotencyKey);
        req.setSourceDepartment("REVENUE");
        req.setTargetDepartment("FOOD");
        req.setPurpose("RATION_ADDRESS_UPDATE");
        req.setRequestedFields(Arrays.asList("citizen.name", "citizen.address"));
        req.setConsent(new CanonicalAddressUpdateRequest.ConsentInfo("CONSENT-00124"));
        req.setCitizen(new CanonicalAddressUpdateRequest.CitizenInfo(
                "CIT-MH-998811",
                "Rajesh Kumar",
                new CanonicalAddressUpdateRequest.AddressInfo("12 M.G. Road, Pune", "DIST-PUN", "TAL-PUN-04")
        ));
        req.setVerification(new CanonicalAddressUpdateRequest.VerificationInfo("VALID", "REVENUE"));
        return req;
    }
}
