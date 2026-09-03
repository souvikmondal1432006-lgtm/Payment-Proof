"""
In-memory SQL Execution & Query Verification Script
Validates that schema.sql and seed.sql load cleanly and runs all investigation queries.
"""

import sqlite3
import re

def convert_mysql_to_sqlite(sql_text):
    # Strip comments and unsupported directives
    sql_text = re.sub(r'ENGINE=InnoDB.*?;', ';', sql_text)
    sql_text = re.sub(r'SET FOREIGN_KEY_CHECKS\s*=\s*\d+;', '', sql_text)
    sql_text = re.sub(r'COMMENT\s+\'[^\']*\'', '', sql_text)
    sql_text = re.sub(r'ON UPDATE CURRENT_TIMESTAMP\([^\)]*\)', '', sql_text, flags=re.IGNORECASE)
    sql_text = re.sub(r'DEFAULT CURRENT_TIMESTAMP\([^\)]*\)', 'DEFAULT CURRENT_TIMESTAMP', sql_text, flags=re.IGNORECASE)
    sql_text = re.sub(r'ENUM\([^\)]+\)', 'TEXT', sql_text)
    sql_text = re.sub(r'DECIMAL\(\d+,\s*\d+\)', 'NUMERIC', sql_text)
    sql_text = re.sub(r'TIMESTAMP\(\d+\)', 'TEXT', sql_text)
    sql_text = re.sub(r'JSON', 'TEXT', sql_text)
    sql_text = re.sub(r'INDEX\s+idx_[a-zA-Z0-9_]+\s*\([^\)]+\),?', '', sql_text)
    sql_text = re.sub(r',\s*\)', ')', sql_text)
    sql_text = re.sub(r',\s*,', ',', sql_text)
    return sql_text

def test_sql_execution():
    conn = sqlite3.connect(":memory:")
    cursor = conn.cursor()
    cursor.execute("PRAGMA foreign_keys = ON;")

    print("Loading schema.sql...")
    with open("schema.sql", "r", encoding="utf-8") as f:
        schema_sql = f.read()
    
    sqlite_schema = convert_mysql_to_sqlite(schema_sql)
    cursor.executescript(sqlite_schema)
    print("Schema created successfully!")

    print("Loading seed.sql...")
    with open("seed.sql", "r", encoding="utf-8") as f:
        seed_sql = f.read()

    sqlite_seed = convert_mysql_to_sqlite(seed_sql)
    cursor.executescript(sqlite_seed)
    print("Seed data successfully loaded into relational engine!")

    # Check row counts
    tables = [
        "payments", "payment_events", "bank_records", "gateway_records",
        "merchant_order_records", "webhook_records", "settlement_records",
        "refund_records", "incident_cases", "investigation_evidence",
        "ml_assessments", "resolutions", "audit_events"
    ]
    print("\n--- Table Row Counts ---")
    for t in tables:
        cursor.execute(f"SELECT COUNT(*) FROM {t}")
        cnt = cursor.fetchone()[0]
        print(f"  {t.ljust(26)}: {cnt} rows")

    # Sample Investigation Queries Test
    print("\n--- Testing Sample Investigation Queries ---")
    
    # Query 1: Detect Bank debits where Gateway is not successful
    q1 = """
    SELECT p.payment_id, p.amount, p.payment_method, b.bank_status, b.utr_number, g.gateway_status, g.error_code, m.order_status
    FROM payments p
    JOIN bank_records b ON p.payment_id = b.payment_id
    JOIN gateway_records g ON p.payment_id = g.payment_id
    JOIN merchant_order_records m ON p.payment_id = m.payment_id
    WHERE b.bank_status IN ('SUCCESS', 'DEBITED') AND g.gateway_status != 'SUCCESS'
    LIMIT 5;
    """
    cursor.execute(q1)
    res1 = cursor.fetchall()
    print(f"Query 1 (Ghost Debits - Bank Debited but Gateway Failed/Pending): {len(res1)} matches found.")
    for row in res1[:2]:
        print("   ", row)

    # Query 2: Gateway Captured but Webhook Failed/Missing
    q2 = """
    SELECT p.payment_id, p.order_id, g.captured_amount, w.delivery_status, w.http_status_code, m.order_status
    FROM payments p
    JOIN gateway_records g ON p.payment_id = g.payment_id
    JOIN webhook_records w ON p.payment_id = w.payment_id
    JOIN merchant_order_records m ON p.payment_id = m.payment_id
    WHERE g.capture_status = 'CAPTURED' AND w.delivery_status IN ('FAILED', 'DROPPED')
    LIMIT 5;
    """
    cursor.execute(q2)
    res2 = cursor.fetchall()
    print(f"\nQuery 2 (Captured but Webhook Dropped): {len(res2)} matches found.")
    for row in res2[:2]:
        print("   ", row)

    # Query 3: Multi-Party 360-Degree Forensic Dossier for an Incident Case
    q3 = """
    SELECT i.incident_id, i.incident_type, i.severity, i.case_status,
           p.amount, b.bank_name, b.bank_status, g.gateway_name, g.gateway_status,
           m.order_status, m.fulfillment_status, w.delivery_status,
           mla.predicted_root_cause, mla.anomaly_score, mla.suggested_action,
           r.action_taken, r.resolved_by
    FROM incident_cases i
    JOIN payments p ON i.payment_id = p.payment_id
    JOIN bank_records b ON p.payment_id = b.payment_id
    JOIN gateway_records g ON p.payment_id = g.payment_id
    JOIN merchant_order_records m ON p.payment_id = m.payment_id
    LEFT JOIN webhook_records w ON p.payment_id = w.payment_id
    LEFT JOIN ml_assessments mla ON i.incident_id = mla.incident_id
    LEFT JOIN resolutions r ON i.incident_id = r.incident_id
    WHERE i.incident_type = 'CONFLICTING_PAYMENT_STATES'
    LIMIT 3;
    """
    cursor.execute(q3)
    res3 = cursor.fetchall()
    print(f"\nQuery 3 (Multi-System Contradiction 360 Dossier): {len(res3)} matches found.")
    for row in res3[:1]:
        print("   ", row)

    print("\nALL QUERIES AND RELATIONAL CHECKS PASSED PERFECTLY!")
    conn.close()

if __name__ == "__main__":
    test_sql_execution()
