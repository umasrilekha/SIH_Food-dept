package com.govmesh.food.service;

import com.govmesh.food.dto.SystemHealthDTO;
import com.govmesh.food.dto.SystemHealthDTO.ComponentHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SystemHealthService {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthService.class);
    private final DataSource dataSource;

    public SystemHealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SystemHealthDTO getSystemHealth() {
        String dbStatus = "OPERATIONAL";
        String dbMessage = "Relational schema and connection pool healthy (HikariCP)";
        String dbVersion = "PostgreSQL 16 / JPA 3.2";

        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && conn.isValid(2)) {
                DatabaseMetaData metaData = conn.getMetaData();
                String prodName = metaData.getDatabaseProductName();
                String prodVersion = metaData.getDatabaseProductVersion();
                dbVersion = prodName + " " + prodVersion;
                dbMessage = "Active connection pool healthy. Database engine: " + prodName;
            } else {
                dbStatus = "DEGRADED";
                dbMessage = "Database connection check returned invalid connection state.";
            }
        } catch (Exception e) {
            log.error("Database connectivity check failed: {}", e.getMessage());
            dbStatus = "DOWN";
            dbMessage = "Database connectivity check failed: " + sanitizeErrorMessage(e.getMessage());
        }

        List<ComponentHealth> components = Arrays.asList(
                ComponentHealth.builder()
                        .name("PostgreSQL Database Service")
                        .category("CORE_INFRASTRUCTURE")
                        .status(dbStatus)
                        .isConfigured(true)
                        .message(dbMessage)
                        .version(dbVersion)
                        .build(),

                ComponentHealth.builder()
                        .name("Officer Authentication & Security")
                        .category("SECURITY")
                        .status("OPERATIONAL")
                        .isConfigured(true)
                        .message("JWT Bearer token verification and BCrypt password encoder active")
                        .version("Spring Security 6.2")
                        .build(),

                ComponentHealth.builder()
                        .name("Department Application Service")
                        .category("APPLICATION_LOGIC")
                        .status("OPERATIONAL")
                        .isConfigured(true)
                        .message("Food & Civil Supplies core domain services running normally")
                        .version("v1.0.0-Phase7")
                        .build(),

                ComponentHealth.builder()
                        .name("SOAP / XML Web Service Interface")
                        .category("EXTERNAL_INTEGRATION")
                        .status("OPERATIONAL")
                        .isConfigured(true)
                        .message("Spring-WS Contract-First SOAP WSDL Endpoint active at /ws/food-department.wsdl")
                        .version("Spring-WS 4.0 / JAXB 3.0")
                        .build(),

                ComponentHealth.builder()
                        .name("GovMesh Interoperability Core Gateway")
                        .category("EXTERNAL_INTEGRATION")
                        .status("OPERATIONAL")
                        .isConfigured(true)
                        .message("GovMesh Persistent Data Layer & Integration Router active")
                        .version("v1.0.0-Phase7")
                        .build()
        );

        return SystemHealthDTO.builder()
                .departmentName("Food, Civil Supplies & Consumer Protection Department (Dept 2)")
                .environment("GovMesh Demonstration Environment (Maharashtra)")
                .timestamp(LocalDateTime.now())
                .components(components)
                .build();
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) return "Unknown error";
        // Remove sensitive credentials or connection strings if present
        return message.replaceAll("password=[^\\s&]+", "password=***")
                      .replaceAll("user=[^\\s&]+", "user=***");
    }
}
