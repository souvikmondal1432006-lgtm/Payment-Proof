import os
import sys
import json
import joblib
import pandas as pd
import numpy as np

# Ensure project root is in sys.path for joblib unpickling
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(CURRENT_DIR)
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from sklearn.metrics import classification_report, confusion_matrix, accuracy_score, precision_recall_fscore_support

def run_evaluation(data_path: str = "data/test.csv", models_dir: str = "models", output_dir: str = "evaluation"):
    if not os.path.exists(data_path):
        print(f"Test dataset not found at {data_path}. Running training pipeline first...")
        from training.train_pipeline import train_and_evaluate_models
        train_and_evaluate_models()

    print(f"Loading test dataset from {data_path}...")
    test_df = pd.read_csv(data_path)
    X_test = test_df.drop(columns=["target_incident_class"])
    y_test = test_df["target_incident_class"]

    preprocessor = joblib.load(os.path.join(models_dir, "preprocessor.joblib"))
    rf_model = joblib.load(os.path.join(models_dir, "incident_classifier_v1.joblib"))
    baseline_model = joblib.load(os.path.join(models_dir, "baseline_model.joblib"))

    X_test_trans = preprocessor.transform(X_test)

    # Evaluate Baseline
    y_pred_base = baseline_model.predict(X_test_trans)
    base_acc = accuracy_score(y_test, y_pred_base)
    base_prec, base_rec, base_f1, _ = precision_recall_fscore_support(y_test, y_pred_base, average="macro", zero_division=0)

    # Evaluate Random Forest
    y_pred_rf = rf_model.predict(X_test_trans)
    rf_acc = accuracy_score(y_test, y_pred_rf)
    rf_prec, rf_rec, rf_f1, _ = precision_recall_fscore_support(y_test, y_pred_rf, average="macro", zero_division=0)

    classes = sorted(list(set(y_test.unique())))
    cm_rf = confusion_matrix(y_test, y_pred_rf, labels=classes)

    print("\n" + "="*80)
    print("PAYMENT INCIDENT ML ENGINE — COMPARATIVE EVALUATION REPORT")
    print("="*80)
    print(f"Total Held-Out Test Samples: {len(X_test)}")
    print(f"Number of Incident Classes : {len(classes)}\n")

    print(f"{'Metric':<25} | {'Baseline (Logistic Reg)':<25} | {'Chosen (Random Forest)':<25}")
    print("-" * 80)
    print(f"{'Overall Accuracy':<25} | {base_acc:<25.4f} | {rf_acc:<25.4f}")
    print(f"{'Macro Precision':<25} | {base_prec:<25.4f} | {rf_prec:<25.4f}")
    print(f"{'Macro Recall':<25} | {base_rec:<25.4f} | {rf_rec:<25.4f}")
    print(f"{'Macro F1-Score':<25} | {base_f1:<25.4f} | {rf_f1:<25.4f}")
    print("="*80)

    print("\n--- PER-CLASS DETAILED PERFORMANCE (Random Forest) ---")
    print(classification_report(y_test, y_pred_rf, labels=classes, zero_division=0))

    print("\n--- CONFUSION MATRIX (Random Forest) ---")
    cm_df = pd.DataFrame(cm_rf, index=[f"Actual: {c}" for c in classes], columns=[f"Pred: {c}" for c in classes])
    print(cm_df.to_string())

    # False Positive Rates
    print("\n--- FALSE POSITIVE RATE (FPR) PER CLASS ---")
    for idx, cls in enumerate(classes):
        fp = cm_rf[:, idx].sum() - cm_rf[idx, idx]
        tn = cm_rf.sum() - (cm_rf[idx, :].sum() + cm_rf[:, idx].sum() - cm_rf[idx, idx])
        fpr = fp / (fp + tn) if (fp + tn) > 0 else 0.0
        print(f"  • {cls:<35}: {fpr:.4f} ({fp} false alarms out of {fp+tn} negatives)")

if __name__ == "__main__":
    run_evaluation()
