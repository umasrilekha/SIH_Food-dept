package com.govmesh.food.govmesh.controller;

import com.govmesh.food.entity.IntegrationAttempt;
import com.govmesh.food.entity.IntegrationTransaction;
import com.govmesh.food.govmesh.config.SoapSimulationConfig;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateRequest;
import com.govmesh.food.govmesh.dto.CanonicalAddressUpdateResponse;
import com.govmesh.food.govmesh.service.GovMeshInteroperabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/govmesh")
public class GovMeshIntegrationController {

    private final GovMeshInteroperabilityService interoperabilityService;
    private final SoapSimulationConfig simulationConfig;

    public GovMeshIntegrationController(GovMeshInteroperabilityService interoperabilityService,
                                        SoapSimulationConfig simulationConfig) {
        this.interoperabilityService = interoperabilityService;
        this.simulationConfig = simulationConfig;
    }

    @PostMapping("/interoperability/address-update")
    @PreAuthorize("hasAnyRole('SERVICE', 'DEPARTMENT_ADMIN', 'FOOD_SUPPLY_OFFICER', 'SENIOR_OFFICER')")
    public ResponseEntity<CanonicalAddressUpdateResponse> processAddressUpdate(
            @RequestBody CanonicalAddressUpdateRequest request) {
        return ResponseEntity.ok(interoperabilityService.processInteroperabilityRequest(request));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('SERVICE', 'FOOD_SUPPLY_OFFICER', 'SENIOR_OFFICER', 'DEPARTMENT_ADMIN', 'AUDITOR')")
    public ResponseEntity<List<IntegrationTransaction>> getTransactions() {
        return ResponseEntity.ok(interoperabilityService.getTransactions());
    }

    @GetMapping("/transactions/retries")
    @PreAuthorize("hasAnyRole('SERVICE', 'FOOD_SUPPLY_OFFICER', 'SENIOR_OFFICER', 'DEPARTMENT_ADMIN', 'AUDITOR')")
    public ResponseEntity<List<IntegrationTransaction>> getFailedAndRetryTransactions() {
        return ResponseEntity.ok(interoperabilityService.getFailedAndRetryTransactions());
    }

    @GetMapping("/transactions/{correlationId}")
    @PreAuthorize("hasAnyRole('SERVICE', 'FOOD_SUPPLY_OFFICER', 'SENIOR_OFFICER', 'DEPARTMENT_ADMIN', 'AUDITOR')")
    public ResponseEntity<IntegrationTransaction> getTransactionByCorrelationId(
            @PathVariable String correlationId) {
        return ResponseEntity.ok(interoperabilityService.getTransactionByCorrelationId(correlationId));
    }

    @GetMapping("/transactions/{correlationId}/attempts")
    @PreAuthorize("hasAnyRole('SERVICE', 'FOOD_SUPPLY_OFFICER', 'SENIOR_OFFICER', 'DEPARTMENT_ADMIN', 'AUDITOR')")
    public ResponseEntity<List<IntegrationAttempt>> getAttemptsByCorrelationId(
            @PathVariable String correlationId) {
        return ResponseEntity.ok(interoperabilityService.getAttemptsByCorrelationId(correlationId));
    }

    @GetMapping("/simulation-mode")
    @PreAuthorize("hasAnyRole('SERVICE', 'FOOD_SUPPLY_OFFICER', 'SENIOR_OFFICER', 'DEPARTMENT_ADMIN', 'AUDITOR')")
    public ResponseEntity<Map<String, String>> getSimulationMode() {
        return ResponseEntity.ok(Map.of("mode", simulationConfig.getMode().name()));
    }

    @PostMapping("/simulation-mode")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SERVICE')")
    public ResponseEntity<Map<String, String>> setSimulationMode(@RequestParam String mode) {
        simulationConfig.setMode(mode);
        return ResponseEntity.ok(Map.of(
                "mode", simulationConfig.getMode().name(),
                "message", "SOAP failure simulation mode updated to: " + simulationConfig.getMode().name()
        ));
    }
}
