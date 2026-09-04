package com.govmesh.food.govmesh.service;

import com.govmesh.food.entity.IntegrationTransaction;
import com.govmesh.food.repository.IntegrationTransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class IntegrationRetryScheduler {

    private final IntegrationTransactionRepository transactionRepository;
    private final GovMeshInteroperabilityService interoperabilityService;

    public IntegrationRetryScheduler(IntegrationTransactionRepository transactionRepository,
                                     GovMeshInteroperabilityService interoperabilityService) {
        this.transactionRepository = transactionRepository;
        this.interoperabilityService = interoperabilityService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processScheduledRetries() {
        LocalDateTime now = LocalDateTime.now();
        List<IntegrationTransaction> dueForRetry = transactionRepository.findByNextRetryAtBeforeAndStatus(now, "RETRYING");

        for (IntegrationTransaction tx : dueForRetry) {
            try {
                interoperabilityService.executeRetry(tx);
            } catch (Exception e) {
                // Log and continue
                System.err.println("Error processing scheduled retry for transaction: " + tx.getCorrelationId() + ": " + e.getMessage());
            }
        }
    }
}
