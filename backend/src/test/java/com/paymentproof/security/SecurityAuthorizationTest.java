package com.paymentproof.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAuthorizationTest {

    @Test
    @DisplayName("Role capabilities enforce proper authorization invariants")
    void testRoleCapabilities() {
        // Investigator
        assertTrue(UserRole.INVESTIGATOR.canResolveIncidents());
        assertTrue(UserRole.INVESTIGATOR.canTriggerInvestigation());
        assertTrue(UserRole.INVESTIGATOR.canVerifyAuditChain());

        // Risk Lead
        assertTrue(UserRole.RISK_LEAD.canResolveIncidents());
        assertTrue(UserRole.RISK_LEAD.canTriggerInvestigation());

        // Auditor (Read-Only)
        assertFalse(UserRole.AUDITOR.canResolveIncidents(), "Auditor cannot resolve incidents");
        assertFalse(UserRole.AUDITOR.canTriggerInvestigation(), "Auditor cannot trigger investigation");
        assertTrue(UserRole.AUDITOR.canVerifyAuditChain(), "Auditor must be allowed to verify audit chain");

        // Admin
        assertTrue(UserRole.ADMIN.canResolveIncidents());
        assertTrue(UserRole.ADMIN.canTriggerInvestigation());
    }
}
