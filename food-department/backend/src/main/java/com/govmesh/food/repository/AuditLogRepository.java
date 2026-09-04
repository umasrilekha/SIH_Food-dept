package com.govmesh.food.repository;

import com.govmesh.food.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop10ByOrderByTimestampDesc();

    List<AuditLog> findByApplicationIdOrderByTimestampDesc(String applicationId);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:appId IS NULL OR :appId = '' OR LOWER(a.applicationId) LIKE LOWER(CONCAT('%', :appId, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
           "(:result IS NULL OR :result = '' OR a.result = :result) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> filterAuditLogs(
            @Param("appId") String appId,
            @Param("action") String action,
            @Param("result") String result
    );
}
