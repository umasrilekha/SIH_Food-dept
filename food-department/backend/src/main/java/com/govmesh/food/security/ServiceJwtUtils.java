package com.govmesh.food.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class ServiceJwtUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceJwtUtils.class);

    private static final List<String> TRUSTED_SERVICES = Arrays.asList("FOOD", "REVENUE", "GOVMESH");

    @Value("${govmesh.service.jwt.secret:GovMeshServiceAccountSecretKey2026SuperSecret}")
    private String serviceJwtSecret;

    @Value("${govmesh.service.api-key:GovMeshSecretServiceKey2026InterDepartment}")
    private String systemApiKey;

    @Value("${govmesh.service.jwt.expiration-ms:86400000}") // 24 Hours
    private long serviceJwtExpirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = serviceJwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateServiceToken(String serviceId, String targetDepartment) {
        if (!isTrustedService(serviceId)) {
            throw new IllegalArgumentException("Cannot generate service token for untrusted service: " + serviceId);
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + serviceJwtExpirationMs);

        return Jwts.builder()
                .setSubject(serviceId)
                .claim("serviceId", serviceId)
                .claim("targetDepartment", targetDepartment)
                .claim("role", "SERVICE")
                .claim("type", "INTER_DEPARTMENT_SERVICE_ACCOUNT")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateServiceToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String serviceId = claims.get("serviceId", String.class);
            String role = claims.get("role", String.class);

            return "SERVICE".equalsIgnoreCase(role) && isTrustedService(serviceId);
        } catch (Exception e) {
            log.error("Invalid inter-department service token: {}", e.getMessage());
            return false;
        }
    }

    public String getServiceIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("serviceId", String.class);
    }

    public boolean validateApiKey(String serviceId, String apiKey) {
        if (!isTrustedService(serviceId) || apiKey == null) {
            return false;
        }
        return systemApiKey.equals(apiKey.trim());
    }

    public boolean isTrustedService(String serviceId) {
        if (serviceId == null) return false;
        return TRUSTED_SERVICES.contains(serviceId.trim().toUpperCase());
    }
}
