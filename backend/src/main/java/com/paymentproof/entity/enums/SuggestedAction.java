package com.paymentproof.entity.enums;

public enum SuggestedAction {
    AUTO_REFUND_CUSTOMER,
    FORCE_SETTLE_MERCHANT,
    RESEND_WEBHOOK,
    MANUAL_BANK_ESCALATION,
    NO_ACTION_REQUIRED,
    HOLD_SETTLEMENT
}
