package com.paymentproof.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Check mutations on resolution endpoint
        if (method.equalsIgnoreCase("POST") && path.matches(".*/api/incidents/.+/resolve.*")) {
            String roleHeader = request.getHeader("X-Operator-Role");
            UserRole role = parseRole(roleHeader);

            if (!role.canResolveIncidents()) {
                log.warn("Unauthorized resolution attempt by role: {} on path: {}", role, path);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"FORBIDDEN\", \"message\": \"Auditor clearance is read-only. Financial state mutation is restricted to Lead Forensic Investigators.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserRole parseRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            return UserRole.INVESTIGATOR; // Default to Investigator in workstation
        }
        try {
            return UserRole.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserRole.INVESTIGATOR;
        }
    }
}
