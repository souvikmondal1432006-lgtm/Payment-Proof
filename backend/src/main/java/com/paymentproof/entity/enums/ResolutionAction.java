package com.paymentproof.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ResolutionAction {
    CUSTOMER_REFUNDED,
    MERCHANT_CREDITED,
    WEBHOOK_RESENT_AND_FULFILLED,
    DUPLICATE_REVERSED,
    TRANSACTION_SETTLED_MANUALLY,
    NO_DISCREPANCY_FOUND,
    ESCALATED_LEGAL_COMPLIANCE;

    @JsonCreator
    public static ResolutionAction fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "AUTO_REFUND_CUSTOMER":
            case "REFUND":
            case "CUSTOMER_REFUNDED":
                return CUSTOMER_REFUNDED;
            case "RESEND_WEBHOOK":
            case "REPLAY_WEBHOOK":
            case "WEBHOOK_RESENT_AND_FULFILLED":
                return WEBHOOK_RESENT_AND_FULFILLED;
            case "FORCE_SETTLE_MERCHANT":
            case "MERCHANT_CREDITED":
                return MERCHANT_CREDITED;
            case "MANUAL_BANK_ESCALATION":
            case "ESCALATED_LEGAL_COMPLIANCE":
            case "ESCALATION":
                return ESCALATED_LEGAL_COMPLIANCE;
            case "DUPLICATE_REVERSED":
            case "REVERSE_DUPLICATE":
                return DUPLICATE_REVERSED;
            case "TRANSACTION_SETTLED_MANUALLY":
            case "MANUAL_SETTLE":
                return TRANSACTION_SETTLED_MANUALLY;
            case "NO_DISCREPANCY_FOUND":
            case "NO_DISCREPANCY":
                return NO_DISCREPANCY_FOUND;
            default:
                for (ResolutionAction action : values()) {
                    if (action.name().equalsIgnoreCase(normalized)) {
                        return action;
                    }
                }
                throw new IllegalArgumentException("Unknown ResolutionAction: " + value);
        }
    }
}
