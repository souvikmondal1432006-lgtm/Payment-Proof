import React, { useState, useEffect } from 'react';
import {
  Server,
  Database,
  Cpu,
  Sparkles,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  ShieldCheck,
  Lock,
  HelpCircle,
  ArrowRight
} from 'lucide-react';
import { api } from '../services/api';

export default function SystemHealthView() {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(false);
  const [lastChecked, setLastChecked] = useState(new Date());
  const [activeTab, setActiveTab] = useState('status'); // 'status' | 'scenarios'
  const [selectedScenarioIndex, setSelectedScenarioIndex] = useState(0);

  useEffect(() => {
    loadHealth();
  }, []);

  const loadHealth = async () => {
    setLoading(true);
    try {
      const res = await api.getSystemHealth();
      setHealth(res);
      setLastChecked(new Date());
    } catch (e) {
      console.error('Failed to load system health:', e);
      setHealth({
        status: 'OFFLINE',
        services: {
          backend: { name: 'JAVA BACKEND', status: 'OFFLINE', label: 'Unavailable' },
          database: { name: 'MYSQL DATABASE', status: 'OFFLINE', label: 'Unavailable' },
          mlService: { name: 'ML SERVICE', status: 'OFFLINE', label: 'Unavailable' },
          gemini: { name: 'GEMINI ASSISTANT', status: 'NOT_CONFIGURED', label: 'Advisory (Not Configured)' }
        }
      });
    } finally {
      setLoading(false);
    }
  };

  const services = [
    {
      id: 'backend',
      title: 'Payment Investigation',
      systemName: 'JAVA BACKEND',
      description: 'Authoritative multi-party reconciliation engine and state consistency evaluator.',
      icon: Server,
      status: health?.services?.backend?.status || 'HEALTHY',
      label: health?.services?.backend?.label || 'Ready'
    },
    {
      id: 'database',
      title: 'Database Records',
      systemName: 'MYSQL DATABASE',
      description: 'Persistent store for payment telemetry, bank records, and SHA-256 audit log.',
      icon: Database,
      status: health?.services?.database?.status || 'HEALTHY',
      label: health?.services?.database?.label || 'Ready'
    },
    {
      id: 'mlService',
      title: 'AI Prediction',
      systemName: 'ML SERVICE',
      description: 'Python Random Forest model estimating incident probabilities and signal weights.',
      icon: Cpu,
      status: health?.services?.mlService?.status || 'HEALTHY',
      label: health?.services?.mlService?.label || 'Ready'
    },
    {
      id: 'gemini',
      title: 'Gemini Explanation Assistant',
      systemName: 'GEMINI ASSISTANT',
      description: 'Advisory explanation assistant generating natural-language case summaries. Optional and advisory only.',
      icon: Sparkles,
      status: health?.services?.gemini?.status || 'NOT_CONFIGURED',
      label: health?.services?.gemini?.label || 'Advisory (Not Configured)'
    }
  ];

  const failureScenarios = [
    {
      id: 'ml_unavailable',
      name: '1. Python ML Service Unavailable',
      whatFailed: 'The Python FastAPI ML inference service is down or unreachable (Connection Refused).',
      whatUserSees: '"AI prediction unavailable — evidence is still available."',
      whatSystemDoes: 'Java continues investigation using deterministic bank/gateway rules. Sets status to NEEDS_REVIEW.',
      dataPreserved: 'All raw ISO-8583 switch logs, gateway callbacks, and timestamps are preserved in database.',
      moneyMovement: 'STRICTLY LOCKED. Zero blind automated re-charges are permitted.',
      recoveryMethod: 'Restart the Python ML service (`npm run start:ml`). Click [AI Re-Assess] to generate ML confidence.'
    },
    {
      id: 'ml_timeout',
      name: '2. Python ML Timeout (>2.5s)',
      whatFailed: 'Python inference process hung or exceeded the 2,500ms WebClient timeout threshold.',
      whatUserSees: '"AI analysis timed out. Evidence is preserved."',
      whatSystemDoes: 'Backend terminates the HTTP request without freezing the UI. Marks investigation as NEEDS_REVIEW.',
      dataPreserved: 'Complete telemetry snapshot and payment metadata remain available in evidence vault.',
      moneyMovement: 'LOCKED. No funds can move automatically.',
      recoveryMethod: 'Inspect ML service worker load or re-trigger investigation once telemetry queue clears.'
    },
    {
      id: 'mysql_unavailable',
      name: '3. MySQL Database Unavailable',
      whatFailed: 'Database connection dropped or HikariCP connection pool exhausted.',
      whatUserSees: '"Payment records could not be loaded. Database connection is temporarily unavailable."',
      whatSystemDoes: 'GlobalExceptionHandler catches DataAccessException and returns safe HTTP 503 without leaking SQL errors or credentials.',
      dataPreserved: 'Database transactions rollback atomically to prevent corrupted half-written records.',
      moneyMovement: 'IMPOSSIBLE. Read/write operations are halted safely.',
      recoveryMethod: 'Restore MySQL database connectivity. Frontend automatically enables [Retry Connection].'
    },
    {
      id: 'backend_offline',
      name: '4. Java Backend Unavailable',
      whatFailed: 'Java Spring Boot backend service stopped or undergoing maintenance.',
      whatUserSees: 'BACKEND OFFLINE banner: "Live payment investigation is temporarily unavailable."',
      whatSystemDoes: 'Frontend halts mutation requests and displays clear offline state with a [Retry Connection] action.',
      dataPreserved: 'Last known session data is preserved and explicitly marked "Last known data — not live".',
      moneyMovement: 'BLOCKED. Zero client-side unauthorized actions can execute.',
      recoveryMethod: 'Start the Spring Boot backend (`npm run start:backend`). Click [Retry Connection].'
    },
    {
      id: 'malformed_ml',
      name: '5. Malformed ML Response',
      whatFailed: 'Python returned invalid JSON, impossible confidence (e.g. 1.5), or unknown incident class.',
      whatUserSees: '"AI prediction unavailable — evidence is still available."',
      whatSystemDoes: 'Java strict validation rejects the response. Rejects saving corrupted ML assessment.',
      dataPreserved: 'Evidence and multi-party records are fully preserved.',
      moneyMovement: 'LOCKED. Safe hold / manual review enforced.',
      recoveryMethod: 'Validate ML schema serializer and deploy verified model artifact.'
    },
    {
      id: 'conflicting_states',
      name: '6. Conflicting Payment States',
      whatFailed: 'Bank reports SUCCESS (₹4,500 debited) but Gateway reports TIMED_OUT and Merchant reports CANCELLED.',
      whatUserSees: '"Payment status is inconsistent. Different payment systems are reporting different states."',
      whatSystemDoes: 'System detects contradiction clash, highlights the divergence in Contradiction Matrix, and prohibits blind retry.',
      dataPreserved: 'Bank UTR, Gateway trace, and Merchant order logs all captured.',
      moneyMovement: 'FROZEN. Auto-refund customer is recommended; blind charging is strictly locked.',
      recoveryMethod: 'Operator verifies Bank UTR and approves instant customer reversal in Resolution Center.'
    },
    {
      id: 'duplicate_investigation',
      name: '7. Duplicate Investigation Request',
      whatFailed: 'Multiple operators or automated webhooks trigger investigation for the same payment simultaneously.',
      whatUserSees: 'Instant response with existing synthesized investigation findings.',
      whatSystemDoes: 'Idempotent cache lookup returns existing assessment without creating duplicate database rows or duplicate ML calls.',
      dataPreserved: 'Audit trail logs OPERATOR_REVIEWED_CASE without corrupting sequential SHA-256 chain.',
      moneyMovement: 'LOCKED.',
      recoveryMethod: 'Normal workflow continues uninterrupted.'
    },
    {
      id: 'missing_events',
      name: '8. Missing Payment Events',
      whatFailed: 'Gateway callback or webhook dropped due to upstream network partition (HTTP 504).',
      whatUserSees: '"Webhook delivery failed after 3 attempts. Gateway captured funds but OMS unnotified."',
      whatSystemDoes: 'Identifies missing link in Connected Evidence Graph and recommends [Resend Webhook].',
      dataPreserved: 'Failed delivery attempts and HTTP status codes recorded.',
      moneyMovement: 'SAFE. No duplicate debit.',
      recoveryMethod: 'Operator clicks "Resend Webhook" or triggers automated merchant callback replay.'
    },
    {
      id: 'settlement_discrepancy',
      name: '9. Incomplete / Discrepant Settlement',
      whatFailed: 'Settlement batch ledger has variance between expected net amount and actual payout amount.',
      whatUserSees: '"Settlement Ledger Variance detected: Expected net INR 8,330, actual INR 8,100."',
      whatSystemDoes: 'Flags settlement discrepancy, places payout on hold pending financial reconciliation.',
      dataPreserved: 'Gross amount, MDR fee, GST tax, and net payout records preserved.',
      moneyMovement: 'PAYOUT HELD to prevent accounting leakage.',
      recoveryMethod: 'Financial auditor reviews fee calculation and executes Force Settle Merchant.'
    },
    {
      id: 'frontend_timeout',
      name: '10. Frontend API Request Timeout',
      whatFailed: 'Slow client network connection causing browser fetch abort.',
      whatUserSees: 'Clean humanized notice: "Investigation request timed out. Payment records remain safe."',
      whatSystemDoes: 'Client prevents button spamming and offers retry action.',
      dataPreserved: 'Server-side state remains untouched.',
      moneyMovement: 'SAFE.',
      recoveryMethod: 'Click [Retry] when network stabilizes.'
    }
  ];

  const getStatusColor = (status) => {
    switch (status) {
      case 'HEALTHY':
        return '#10b981';
      case 'DEGRADED':
        return '#f59e0b';
      case 'NOT_CONFIGURED':
        return '#94a3b8';
      case 'OFFLINE':
      default:
        return '#ef4444';
    }
  };

  const getStatusBg = (status) => {
    switch (status) {
      case 'HEALTHY':
        return 'rgba(16, 185, 129, 0.1)';
      case 'DEGRADED':
        return 'rgba(245, 158, 11, 0.1)';
      case 'NOT_CONFIGURED':
        return 'rgba(148, 163, 184, 0.1)';
      case 'OFFLINE':
      default:
        return 'rgba(239, 68, 68, 0.1)';
    }
  };

  const getStatusBorder = (status) => {
    switch (status) {
      case 'HEALTHY':
        return 'rgba(16, 185, 129, 0.3)';
      case 'DEGRADED':
        return 'rgba(245, 158, 11, 0.3)';
      case 'NOT_CONFIGURED':
        return 'rgba(148, 163, 184, 0.3)';
      case 'OFFLINE':
      default:
        return 'rgba(239, 68, 68, 0.3)';
    }
  };

  const selectedScenario = failureScenarios[selectedScenarioIndex];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span className="badge badge-white">SYSTEM STATUS</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>SERVICE HEALTH & FAILURE ENGINEERING</span>
          </div>
          <h1 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff' }}>
            System Health & Failure Safety
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '2px' }}>
            Live status of the 3 primary services and fail-safe behavioral guarantees under adverse conditions.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ display: 'flex', background: '#121212', borderRadius: '8px', padding: '3px', border: '1px solid var(--border-subtle)' }}>
            <button
              onClick={() => setActiveTab('status')}
              style={{
                padding: '6px 14px',
                borderRadius: '6px',
                border: 'none',
                background: activeTab === 'status' ? '#27272a' : 'transparent',
                color: activeTab === 'status' ? '#ffffff' : 'var(--text-dim)',
                fontSize: '0.8rem',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              3 Core Services
            </button>
            <button
              onClick={() => setActiveTab('scenarios')}
              style={{
                padding: '6px 14px',
                borderRadius: '6px',
                border: 'none',
                background: activeTab === 'scenarios' ? '#27272a' : 'transparent',
                color: activeTab === 'scenarios' ? '#ffffff' : 'var(--text-dim)',
                fontSize: '0.8rem',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              10 Failure Scenarios
            </button>
          </div>

          <button
            onClick={loadHealth}
            disabled={loading}
            className="btn btn-outline-white btn-sm"
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      {activeTab === 'status' ? (
        <>
          {/* Main 3 Service Cards */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '18px'
          }}>
            {services.map((svc) => {
              const Icon = svc.icon;
              const color = getStatusColor(svc.status);
              const bg = getStatusBg(svc.status);
              const border = getStatusBorder(svc.status);

              return (
                <div
                  key={svc.id}
                  style={{
                    background: '#0a0a0a',
                    border: `1px solid ${border}`,
                    borderRadius: '14px',
                    padding: '24px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    gap: '20px',
                    boxShadow: `0 4px 20px ${bg}`
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{
                      width: '44px',
                      height: '44px',
                      borderRadius: '10px',
                      background: bg,
                      border: `1px solid ${border}`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: color
                    }}>
                      <Icon size={22} />
                    </div>

                    <div style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '6px',
                      background: bg,
                      border: `1px solid ${border}`,
                      borderRadius: '20px',
                      padding: '4px 12px',
                      fontSize: '0.78rem',
                      fontWeight: 700,
                      color: color
                    }}>
                      <span style={{
                        width: '8px',
                        height: '8px',
                        borderRadius: '50%',
                        background: color,
                        boxShadow: `0 0 8px ${color}`
                      }} />
                      {svc.status}
                    </div>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-dim)', letterSpacing: '0.5px' }}>
                      {svc.systemName}
                    </div>
                    <div style={{ fontSize: '1.2rem', fontWeight: 800, color: '#ffffff', marginTop: '2px' }}>
                      {svc.title}
                    </div>
                    <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: 1.5, marginTop: '8px' }}>
                      {svc.description}
                    </p>
                  </div>

                  <div style={{
                    borderTop: '1px solid var(--border-subtle)',
                    paddingTop: '12px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    fontSize: '0.78rem'
                  }}>
                    <span style={{ color: 'var(--text-dim)' }}>Operational State:</span>
                    <span style={{ fontWeight: 700, color: color }}>● {svc.label}</span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Money Safety Guarantee Banner */}
          <div style={{
            background: 'rgba(59, 130, 246, 0.06)',
            border: '1px solid rgba(59, 130, 246, 0.25)',
            borderRadius: '12px',
            padding: '20px 24px',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px'
          }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              background: 'rgba(59, 130, 246, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#60a5fa',
              flexShrink: 0
            }}>
              <Lock size={20} />
            </div>
            <div>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff', marginBottom: '4px' }}>
                Money Safety Invariant: Safe Hold Over Blind Automation
              </div>
              <div style={{ fontSize: '0.84rem', color: '#93c5fd', lineHeight: 1.5 }}>
                Whenever payment truth is uncertain (due to offline ML, bank latency, or webhook drops), the system strictly prefers <strong style={{ color: '#ffffff' }}>NEEDS_REVIEW / SAFE HOLD</strong>. Blind automated customer re-charges are cryptographically locked.
              </div>
            </div>
          </div>
        </>
      ) : (
        /* 10 Failure Scenarios Explorer */
        <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: '20px' }}>
          {/* Scenario List */}
          <div style={{
            background: '#0a0a0a',
            border: '1px solid var(--border-subtle)',
            borderRadius: '12px',
            padding: '12px',
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            maxHeight: '600px',
            overflowY: 'auto'
          }}>
            {failureScenarios.map((sc, idx) => {
              const isSelected = selectedScenarioIndex === idx;
              return (
                <button
                  key={sc.id}
                  onClick={() => setSelectedScenarioIndex(idx)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: 'none',
                    background: isSelected ? '#1f1f23' : 'transparent',
                    color: isSelected ? '#ffffff' : 'var(--text-muted)',
                    fontSize: '0.82rem',
                    fontWeight: isSelected ? 700 : 500,
                    textAlign: 'left',
                    cursor: 'pointer',
                    transition: 'all 0.15s'
                  }}
                >
                  <span>{sc.name}</span>
                  {isSelected && <ArrowRight size={14} style={{ color: '#3b82f6' }} />}
                </button>
              );
            })}
          </div>

          {/* Scenario Details */}
          <div style={{
            background: '#0a0a0a',
            border: '1px solid var(--border-subtle)',
            borderRadius: '12px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '18px'
          }}>
            <div>
              <span className="badge badge-white" style={{ fontSize: '0.7rem', marginBottom: '8px' }}>
                FAILURE MATRIX SPECIFICATION
              </span>
              <h2 className="font-display" style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ffffff' }}>
                {selectedScenario.name}
              </h2>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div style={{ background: '#121212', padding: '14px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#ef4444', textTransform: 'uppercase', marginBottom: '4px' }}>
                  What Failed
                </div>
                <div style={{ fontSize: '0.85rem', color: '#ffffff', lineHeight: 1.4 }}>
                  {selectedScenario.whatFailed}
                </div>
              </div>

              <div style={{ background: '#121212', padding: '14px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#60a5fa', textTransform: 'uppercase', marginBottom: '4px' }}>
                  What User Sees
                </div>
                <div style={{ fontSize: '0.85rem', color: '#ffffff', lineHeight: 1.4 }}>
                  {selectedScenario.whatUserSees}
                </div>
              </div>

              <div style={{ background: '#121212', padding: '14px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#f59e0b', textTransform: 'uppercase', marginBottom: '4px' }}>
                  What System Does
                </div>
                <div style={{ fontSize: '0.85rem', color: '#ffffff', lineHeight: 1.4 }}>
                  {selectedScenario.whatSystemDoes}
                </div>
              </div>

              <div style={{ background: '#121212', padding: '14px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#10b981', textTransform: 'uppercase', marginBottom: '4px' }}>
                  What Data Is Preserved
                </div>
                <div style={{ fontSize: '0.85rem', color: '#ffffff', lineHeight: 1.4 }}>
                  {selectedScenario.dataPreserved}
                </div>
              </div>
            </div>

            <div style={{ background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.25)', padding: '14px 18px', borderRadius: '8px' }}>
              <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#ef4444', textTransform: 'uppercase', marginBottom: '4px' }}>
                Whether Money Can Move
              </div>
              <div style={{ fontSize: '0.88rem', color: '#fca5a5', fontWeight: 600 }}>
                {selectedScenario.moneyMovement}
              </div>
            </div>

            <div style={{ background: '#121212', padding: '14px 18px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
              <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                Recovery Method
              </div>
              <div style={{ fontSize: '0.85rem', color: '#d4d4d8', lineHeight: 1.4 }}>
                {selectedScenario.recoveryMethod}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
