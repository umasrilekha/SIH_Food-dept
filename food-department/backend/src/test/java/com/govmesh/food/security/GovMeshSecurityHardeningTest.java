package com.govmesh.food.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govmesh.food.dto.AuthDTOs.LoginRequest;
import com.govmesh.food.entity.Consent;
import com.govmesh.food.entity.User;
import com.govmesh.food.govmesh.adapter.FoodDepartmentAdapter;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateRequest;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateResponse;
import com.govmesh.food.govmesh.mapper.FoodDepartmentSchemaMapper;
import com.govmesh.food.repository.ConsentRepository;
import com.govmesh.food.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GovMeshSecurityHardeningTest {

    @TestConfiguration
    static class TestAdapterConfig {
        @Bean
        @Primary
        public FoodDepartmentAdapter foodDepartmentAdapter(FoodDepartmentSchemaMapper schemaMapper) {
            return new FoodDepartmentAdapter(schemaMapper) {
                @Override
                public CanonicalAddressUpdateResponse sendAddressUpdate(CanonicalAddressUpdateRequest canonicalRequest, String soapEndpointUrl) {
                    return CanonicalAddressUpdateResponse.builder()
                            .applicationId(canonicalRequest != null ? canonicalRequest.getApplicationId() : "GM-SEC-06")
                            .status("SUCCESS")
                            .message("Address update accepted by Food Department")
                            .correlationId(canonicalRequest != null ? canonicalRequest.getCorrelationId() : "CORR-SEC-06")
                            .targetDepartment("FOOD")
                            .build();
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceJwtUtils serviceJwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String officerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Optional<User> officerOpt = userRepository.findByUsername("food.officer");
        if (officerOpt.isPresent()) {
            UserPrincipal officerPrincipal = UserPrincipal.create(officerOpt.get());
            officerToken = jwtUtils.generateJwtToken(officerPrincipal);
        }

        Optional<User> adminOpt = userRepository.findByUsername("food.admin");
        if (adminOpt.isPresent()) {
            UserPrincipal adminPrincipal = UserPrincipal.create(adminOpt.get());
            adminToken = jwtUtils.generateJwtToken(adminPrincipal);
        }
    }

    @Test
    @DisplayName("1. Unauthenticated request to protected API -> 401 Unauthorized")
    void test1_UnauthenticatedProtectedEndpoint_Rejected() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Valid officer JWT -> 200 OK Accepted")
    void test2_ValidOfficerJwt_Accepted() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("3. Expired officer JWT -> 401 Unauthorized")
    void test3_ExpiredOfficerJwt_Rejected() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb29kLm9mZmljZXIiLCJleHAiOjE2MDAwMDAwMDB9.invalid_signature";
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. Malformed officer JWT -> 401 Unauthorized")
    void test4_MalformedOfficerJwt_Rejected() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer THIS_IS_NOT_A_VALID_JWT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Insufficient officer role -> 403 Forbidden")
    void test5_InsufficientOfficerRole_Rejected() throws Exception {
        // Ordinary officer trying to call Admin-only simulation-mode toggle
        mockMvc.perform(post("/api/govmesh/simulation-mode")
                        .param("mode", "TIMEOUT")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Authorized role (Department Admin) -> 200 OK Accepted")
    void test6_AuthorizedRole_Accepted() throws Exception {
        mockMvc.perform(post("/api/govmesh/simulation-mode")
                        .param("mode", "SUCCESS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SUCCESS"));
    }

    @Test
    @DisplayName("7. Invalid consent -> Consent Gatekeeper Blocked")
    void test7_InvalidConsent_Rejected() throws Exception {
        CanonicalAddressUpdateRequest req = createSampleRequest("GM-SEC-01", "NON-EXISTENT-CONSENT");

        mockMvc.perform(post("/api/govmesh/interoperability/address-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.errorCode").value("CONSENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("8. Expired consent -> Consent Gatekeeper Blocked")
    void test8_ExpiredConsent_Rejected() throws Exception {
        CanonicalAddressUpdateRequest req = createSampleRequest("GM-SEC-02", "CONSENT-EXPIRED-001");

        mockMvc.perform(post("/api/govmesh/interoperability/address-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.errorCode").value("CONSENT_EXPIRED"));
    }

    @Test
    @DisplayName("9. Revoked consent -> Consent Gatekeeper Blocked")
    void test9_RevokedConsent_Rejected() throws Exception {
        CanonicalAddressUpdateRequest req = createSampleRequest("GM-SEC-03", "CONSENT-REVOKED-001");

        mockMvc.perform(post("/api/govmesh/interoperability/address-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("10. Unauthorized data field -> Consent Gatekeeper Blocked (FIELD_NOT_PERMITTED)")
    void test10_UnauthorizedDataField_Rejected() throws Exception {
        CanonicalAddressUpdateRequest req = createSampleRequest("GM-SEC-04", "CONSENT-00124");
        req.setRequestedFields(Arrays.asList("citizen.name", "citizen.financialInformation"));

        mockMvc.perform(post("/api/govmesh/interoperability/address-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.errorCode").value("FIELD_NOT_PERMITTED"));
    }

    @Test
    @DisplayName("11. Untrusted inter-department request (missing service token/key) -> 401 Unauthorized")
    void test11_UntrustedInterDepartmentRequest_Rejected() throws Exception {
        CanonicalAddressUpdateRequest req = createSampleRequest("GM-SEC-05", "CONSENT-00124");

        // Calling without Bearer officer token or X-GovMesh-Service-Token / API Key
        mockMvc.perform(post("/api/govmesh/interoperability/address-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. Valid trusted inter-department service account token -> 200 OK Accepted")
    void test12_ValidTrustedInterDepartmentServiceToken_Accepted() throws Exception {
        String serviceToken = serviceJwtUtils.generateServiceToken("REVENUE", "FOOD");
        CanonicalAddressUpdateRequest req = createSampleRequest("GM-SEC-06", "CONSENT-00124");

        mockMvc.perform(post("/api/govmesh/interoperability/address-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-GovMesh-Service-Token", serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("13. CORS trusted origin -> Allowed")
    void test13_CorsTrustedOrigin_Accepted() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("14. CORS untrusted origin -> Rejected by CORS filter")
    void test14_CorsUntrustedOrigin_Rejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Origin", "http://malicious-attacker.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("15. Error response sanitization -> Stack traces masked")
    void test15_ErrorResponseSanitization() throws Exception {
        mockMvc.perform(get("/api/applications/9999999")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Application not found")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("16. Passwords never exposed in user API DTO responses")
    void test16_PasswordsNeverExposedInDto() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("food.officer"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("17. Valid Login Authentication with BCrypt -> Returns JWT")
    void test17_ValidLogin_ReturnsJwtToken() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername("food.officer");
        login.setPassword("Food@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("food.officer"));
    }

    private CanonicalAddressUpdateRequest createSampleRequest(String appId, String consentId) {
        CanonicalAddressUpdateRequest req = new CanonicalAddressUpdateRequest();
        req.setApplicationId(appId);
        req.setCorrelationId("CORR-SEC-" + System.currentTimeMillis());
        req.setIdempotencyKey(appId + ":ADDRESS_UPDATE");
        req.setSourceDepartment("REVENUE");
        req.setTargetDepartment("FOOD");
        req.setPurpose("RATION_ADDRESS_UPDATE");
        req.setRequestedFields(Arrays.asList("citizen.name", "citizen.address"));
        req.setConsent(new CanonicalAddressUpdateRequest.ConsentInfo(consentId));
        req.setCitizen(new CanonicalAddressUpdateRequest.CitizenInfo(
                "CIT-MH-998811",
                "Rajesh Kumar",
                new CanonicalAddressUpdateRequest.AddressInfo("12 M.G. Road, Pune", "DIST-PUN", "TAL-PUN-04")
        ));
        req.setVerification(new CanonicalAddressUpdateRequest.VerificationInfo("VALID", "REVENUE"));
        return req;
    }
}
