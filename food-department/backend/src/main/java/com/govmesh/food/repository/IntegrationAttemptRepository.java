package com.govmesh.food.repository;

import com.govmesh.food.entity.IntegrationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationAttemptRepository extends JpaRepository<IntegrationAttempt, Long> {
    List<IntegrationAttempt> findByCorrelationIdOrderByAttemptNumberAsc(String correlationId);
    List<IntegrationAttempt> findByApplicationIdOrderByAttemptNumberAsc(String applicationId);
}
