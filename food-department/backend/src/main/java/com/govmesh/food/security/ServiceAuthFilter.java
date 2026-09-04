package com.govmesh.food.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class ServiceAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthFilter.class);

    private final ServiceJwtUtils serviceJwtUtils;

    public ServiceAuthFilter(ServiceJwtUtils serviceJwtUtils) {
        this.serviceJwtUtils = serviceJwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/govmesh")) {
            try {
                String serviceToken = parseServiceToken(request);
                String serviceId = request.getHeader("X-GovMesh-Service-Id");
                String apiKey = request.getHeader("X-GovMesh-API-Key");

                if (StringUtils.hasText(serviceToken) && serviceJwtUtils.validateServiceToken(serviceToken)) {
                    String trustedServiceId = serviceJwtUtils.getServiceIdFromToken(serviceToken);
                    setServiceAuthentication(trustedServiceId, request);
                } else if (StringUtils.hasText(serviceId) && StringUtils.hasText(apiKey) && serviceJwtUtils.validateApiKey(serviceId, apiKey)) {
                    setServiceAuthentication(serviceId, request);
                }
            } catch (Exception e) {
                log.error("Failed to authenticate inter-department service request: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setServiceAuthentication(String serviceId, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "SERVICE:" + serviceId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String parseServiceToken(HttpServletRequest request) {
        String serviceTokenHeader = request.getHeader("X-GovMesh-Service-Token");
        if (StringUtils.hasText(serviceTokenHeader)) {
            if (serviceTokenHeader.startsWith("Bearer ")) {
                return serviceTokenHeader.substring(7);
            }
            return serviceTokenHeader.trim();
        }
        return null;
    }
}
