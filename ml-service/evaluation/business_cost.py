"""
Financial Business Cost Analysis Module
Calculates the operational and financial impact of False Positives (FP) and False Negatives (FN)
across all payment incident classes for both Baseline and Chosen models.
"""

import os
import json
import numpy as np
import pandas as pd

# Financial cost penalties in INR for business error categories
COST_WEIGHTS = {
    "NORMAL_FALSE_ALARM": {
        "cost_inr": 150.0,
        "description": "Operational overhead of triage engineer investigating a normal transaction"
    },
    "MISSED_GHOST_DEBIT": {
        "cost_inr": 1200.0,
        "description": "Regulatory escalation, customer support penalty, ombudsman fee for unaddressed bank debit"
    },
    "FALSE_GHOST_DEBIT_REFUND": {
        "cost_inr": 2500.0,
        "description": "Merchant financial loss from issuing customer refund when goods/services were already fulfilled"
    },
    "MISSED_MISSING_WEBHOOK": {
        "cost_inr": 500.0,
        "description": "Customer drop-off/churn cost from unfulfilled order despite successful capture"
    },
    "FALSE_DUPLICATE_REVERSAL": {
        "cost_inr": 400.0,
        "description": "Customer friction and administrative cost from reversing valid distinct order"
    },
    "GENERAL_MISCLASSIFICATION": {
        "cost_inr": 300.0,
        "description": "Base latency delay and cross-desk routing cost for misrouted incident"
    }
}

def analyze_business_cost(confusion_matrix_path: str = "evaluation/confusion_matrix.json"):
    if not os.path.exists(confusion_matrix_path):
        print(f"Confusion matrix artifact not found at {confusion_matrix_path}")
        return

    with open(confusion_matrix_path, "r") as f:
        cm_data = json.load(f)

    labels = cm_data["labels"]
    cm_base = np.array(cm_data["confusion_matrix_baseline"])
    cm_rf = np.array(cm_data["confusion_matrix_random_forest"])

    def evaluate_cost_breakdown(cm):
        breakdown = {
            "normal_false_alarms": 0,
            "missed_ghost_debits": 0,
            "false_ghost_debit_refunds": 0,
            "missed_missing_webhooks": 0,
            "false_duplicate_reversals": 0,
            "other_misclassifications": 0,
            "total_cost_inr": 0.0
        }

        for i, actual in enumerate(labels):
            for j, predicted in enumerate(labels):
                if i != j:
                    count = int(cm[i, j])
                    if actual == "NORMAL" and predicted != "NORMAL":
                        breakdown["normal_false_alarms"] += count
                        breakdown["total_cost_inr"] += count * COST_WEIGHTS["NORMAL_FALSE_ALARM"]["cost_inr"]
                    elif actual == "BANK_DEBIT_GATEWAY_FAILURE" and predicted != "BANK_DEBIT_GATEWAY_FAILURE":
                        breakdown["missed_ghost_debits"] += count
                        breakdown["total_cost_inr"] += count * COST_WEIGHTS["MISSED_GHOST_DEBIT"]["cost_inr"]
                    elif actual != "BANK_DEBIT_GATEWAY_FAILURE" and predicted == "BANK_DEBIT_GATEWAY_FAILURE":
                        breakdown["false_ghost_debit_refunds"] += count
                        breakdown["total_cost_inr"] += count * COST_WEIGHTS["FALSE_GHOST_DEBIT_REFUND"]["cost_inr"]
                    elif actual == "MISSING_WEBHOOK":
                        breakdown["missed_missing_webhooks"] += count
                        breakdown["total_cost_inr"] += count * COST_WEIGHTS["MISSED_MISSING_WEBHOOK"]["cost_inr"]
                    elif predicted == "DUPLICATE_PAYMENT":
                        breakdown["false_duplicate_reversals"] += count
                        breakdown["total_cost_inr"] += count * COST_WEIGHTS["FALSE_DUPLICATE_REVERSAL"]["cost_inr"]
                    else:
                        breakdown["other_misclassifications"] += count
                        breakdown["total_cost_inr"] += count * COST_WEIGHTS["GENERAL_MISCLASSIFICATION"]["cost_inr"]
        return breakdown

    base_cost = evaluate_cost_breakdown(cm_base)
    rf_cost = evaluate_cost_breakdown(cm_rf)

    savings_inr = base_cost["total_cost_inr"] - rf_cost["total_cost_inr"]
    savings_pct = (savings_inr / base_cost["total_cost_inr"] * 100) if base_cost["total_cost_inr"] > 0 else 0.0

    print("\n" + "="*80)
    print("FINANCIAL BUSINESS COST & RISK REDUCTION ANALYSIS")
    print("="*80)
    print(f"{'Error Category':<32} | {'Cost Weight':<12} | {'Baseline Count':<14} | {'Random Forest Count':<18}")
    print("-" * 80)
    print(f"{'Normal False Alarms':<32} | {'INR 150':<12} | {base_cost['normal_false_alarms']:<14} | {rf_cost['normal_false_alarms']:<18}")
    print(f"{'Missed Ghost Debits':<32} | {'INR 1,200':<12} | {base_cost['missed_ghost_debits']:<14} | {rf_cost['missed_ghost_debits']:<18}")
    print(f"{'False Ghost Debit Refunds':<32} | {'INR 2,500':<12} | {base_cost['false_ghost_debit_refunds']:<14} | {rf_cost['false_ghost_debit_refunds']:<18}")
    print(f"{'Missed Missing Webhooks':<32} | {'INR 500':<12} | {base_cost['missed_missing_webhooks']:<14} | {rf_cost['missed_missing_webhooks']:<18}")
    print(f"{'False Duplicate Reversals':<32} | {'INR 400':<12} | {base_cost['false_duplicate_reversals']:<14} | {rf_cost['false_duplicate_reversals']:<18}")
    print(f"{'Other Misclassifications':<32} | {'INR 300':<12} | {base_cost['other_misclassifications']:<14} | {rf_cost['other_misclassifications']:<18}")
    print("=" * 80)
    base_cost_str = f"INR {base_cost['total_cost_inr']:,.2f}"
    rf_cost_str = f"INR {rf_cost['total_cost_inr']:,.2f}"
    savings_str = f"INR {savings_inr:,.2f} (-{savings_pct:.1f}%)"

    print(f"{'TOTAL EXPECTED FINANCIAL ERROR COST':<46} | {base_cost_str:<14} | {rf_cost_str:<18}")
    print(f"{'FINANCIAL RISK EXPOSURE REDUCTION':<46} | {'--':<14} | {savings_str:<18}")
    print("=" * 80)

if __name__ == "__main__":
    analyze_business_cost()
