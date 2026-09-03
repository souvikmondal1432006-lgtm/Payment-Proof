package com.paymentproof.entity.enums;

public enum WebhookDeliveryStatus {
    SCHEDULED,
    PENDING,
    DELIVERED,
    FAILED,
    DROPPED,
    TIMED_OUT,
    RETRYING
}
