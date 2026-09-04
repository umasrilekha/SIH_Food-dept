package com.govmesh.food.repository;

import com.govmesh.food.entity.IntegrationTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationTransactionRepository extends JpaRepository<IntegrationTransaction, Long> {

    Optional<IntegrationTransaction> findByCorrelationId(String correlationId);

    Optional<IntegrationTransaction> findByIdempotencyKey(String idempotencyKey);

    List<IntegrationTransaction> findByApplicationIdOrderByStartedAtDesc(String applicationId);

    @Query("SELECT it FROM IntegrationTransaction it ORDER BY it.startedAt DESC")
    List<IntegrationTransaction> findAllOrderedByStartedAtDesc();

    List<IntegrationTransaction> findByStatusInOrderByStartedAtDesc(List<String> statuses);

    List<IntegrationTransaction> findByNextRetryAtBeforeAndStatus(LocalDateTime now, String status);
}
