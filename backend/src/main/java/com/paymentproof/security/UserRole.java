package com.paymentproof.security;

public enum UserRole {
    INVESTIGATOR,
    RISK_LEAD,
    AUDITOR,
    ADMIN;

    public boolean canResolveIncidents() {
        return this == INVESTIGATOR || this == RISK_LEAD || this == ADMIN;
    }

    public boolean canTriggerInvestigation() {
        return this == INVESTIGATOR || this == RISK_LEAD || this == ADMIN;
    }

    public boolean canVerifyAuditChain() {
        return true; // All authenticated roles can verify audit chain
    }
}
