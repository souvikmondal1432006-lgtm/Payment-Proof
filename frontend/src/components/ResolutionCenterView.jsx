import React, { useState } from 'react';
import {
  ShieldCheck,
  ShieldAlert,
  RotateCcw,
  Send,
  Lock,
  CheckCircle2,
  AlertTriangle,
  FileText,
  Check,
  ArrowRight
} from 'lucide-react';
import { api } from '../services/api';

export default function ResolutionCenterView({
  incident,
  currentUser,
  onResolved
}) {
  const [selectedAction, setSelectedAction] = useState('AUTO_REFUND_CUSTOMER');
  const [notes, setNotes] = useState('');
  const [resolving, setResolving] = useState(false);
  const [successResult, setSuccessResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  if (!incident) return null;

  const isBankDebited = incident.bank?.status === 'SUCCESS' || incident.bank?.status === 'DEBITED';

  const ACTION_MAPPING = {
    AUTO_REFUND_CUSTOMER: 'CUSTOMER_REFUNDED',
    CUSTOMER_REFUNDED: 'CUSTOMER_REFUNDED',
    RESEND_WEBHOOK: 'WEBHOOK_RESENT_AND_FULFILLED',
    WEBHOOK_RESENT_AND_FULFILLED: 'WEBHOOK_RESENT_AND_FULFILLED',
    FORCE_SETTLE_MERCHANT: 'MERCHANT_CREDITED',
    MERCHANT_CREDITED: 'MERCHANT_CREDITED',
    MANUAL_BANK_ESCALATION: 'ESCALATED_LEGAL_COMPLIANCE',
    ESCALATED_LEGAL_COMPLIANCE: 'ESCALATED_LEGAL_COMPLIANCE',
    DUPLICATE_REVERSED: 'DUPLICATE_REVERSED',
    NO_DISCREPANCY_FOUND: 'NO_DISCREPANCY_FOUND'
  };

  const handleExecute = async () => {
    setErrorMessage(null);
    setResolving(true);
    try {
      const targetId = incident.incidentId || incident.id || incident.paymentId;
      const mappedAction = ACTION_MAPPING[selectedAction] || selectedAction;
      const payload = {
        actionTaken: mappedAction,
        resolutionType: 'OPERATOR_MANUAL_OVERRIDE',
        resolvedBy: currentUser?.name || currentUser?.id || 'LEAD_INVESTIGATOR',
        resolutionNotes: notes || `Authoritative remediation executed for incident ${targetId}.`,
        notes: notes || `Authoritative remediation executed for incident ${targetId}.`,
        financialImpactAmount: Number(incident.amount || 0),
        liabilityParty: 'PLATFORM_LOSS_NONE'
      };
      const res = await api.resolveIncident(targetId, payload);
      setSuccessResult(res);
      if (onResolved) onResolved(res);
    } catch (e) {
      console.error('Failed to execute resolution:', e);
      setErrorMessage(e.humanMessage || e.message || 'Failed to execute authoritative resolution.');
    } finally {
      setResolving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
          <span className="badge badge-outline font-mono">{incident.paymentId}</span>
          <span className="badge badge-white">RESOLUTION WORKSTATION</span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>AUTHORITATIVE INCIDENT REMEDIATION</span>
        </div>
        <h1 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff' }}>
          Incident Resolution & Financial Remediation
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '2px' }}>
          Execute safe state reconciliations, customer refunds, or webhook replays with immutable audit logging.
        </p>
      </div>

      {successResult ? (
        /* Success Confirmation */
        <div style={{
          background: 'rgba(16, 185, 129, 0.08)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          borderRadius: '12px',
          padding: '32px',
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '14px'
        }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '50%',
            background: '#10b981',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#000000'
          }}>
            <Check size={28} strokeWidth={3} />
          </div>
          <div className="font-display" style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ffffff' }}>
            Remediation Executed Successfully
          </div>
          <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', maxWidth: '500px' }}>
            Action <span className="font-mono" style={{ color: '#10b981', fontWeight: 700 }}>{successResult.actionTaken}</span> applied.
            Immutable audit record saved with ID: <span className="font-mono" style={{ color: '#ffffff' }}>{successResult.resolutionId}</span>.
          </div>
          <button
            onClick={() => setSuccessResult(null)}
            className="btn btn-outline-white btn-sm"
            style={{ marginTop: '8px' }}
          >
            Perform Another Action
          </button>
        </div>
      ) : (
        /* Action Execution Form */
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 1fr) 340px', gap: '20px' }}>
          {/* Action Options */}
          <div style={{
            background: '#0a0a0a',
            border: '1px solid var(--border-subtle)',
            borderRadius: '12px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#ffffff' }}>
              Select Operational Action:
            </div>

            {/* Action 1: Auto-Refund */}
            <div
              onClick={() => setSelectedAction('AUTO_REFUND_CUSTOMER')}
              style={{
                background: selectedAction === 'AUTO_REFUND_CUSTOMER' ? '#18181b' : '#121212',
                border: selectedAction === 'AUTO_REFUND_CUSTOMER' ? '1px solid #ffffff' : '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '16px',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                  1. Auto-Refund Customer (Recommended)
                </span>
                <span className="badge badge-success">SAFEST</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                Issues instant bank credit reversal of ₹{Number(incident.amount || 0).toLocaleString('en-IN')} under ARN to original payment instrument.
              </div>
            </div>

            {/* Action 2: Resend Webhook */}
            <div
              onClick={() => setSelectedAction('RESEND_WEBHOOK')}
              style={{
                background: selectedAction === 'RESEND_WEBHOOK' ? '#18181b' : '#121212',
                border: selectedAction === 'RESEND_WEBHOOK' ? '1px solid #ffffff' : '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '16px',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                  2. Replay Asynchronous Webhook
                </span>
                <span className="badge badge-outline">MERCHANT SYNC</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                Re-dispatches signed HMAC payment.captured webhook payload to merchant endpoint ({incident.webhook?.targetUrl || 'merchant server'}).
              </div>
            </div>

            {/* Action 3: Force Settle Merchant */}
            <div
              onClick={() => setSelectedAction('FORCE_SETTLE_MERCHANT')}
              style={{
                background: selectedAction === 'FORCE_SETTLE_MERCHANT' ? '#18181b' : '#121212',
                border: selectedAction === 'FORCE_SETTLE_MERCHANT' ? '1px solid #ffffff' : '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '16px',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                  3. Force Settle Merchant Ledger
                </span>
                <span className="badge badge-outline">LEDGER OVERRIDE</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                Reconciles fee deduction variance and queues payout for next bank nodal settlement batch.
              </div>
            </div>

            {/* Action 4: Blind Retry (LOCKED IF BANK DEBITED) */}
            <div
              style={{
                background: isBankDebited ? 'rgba(239, 68, 68, 0.05)' : '#121212',
                border: isBankDebited ? '1px solid rgba(239, 68, 68, 0.25)' : '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '16px',
                opacity: isBankDebited ? 0.7 : 1,
                cursor: isBankDebited ? 'not-allowed' : 'pointer'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Lock size={15} style={{ color: '#ef4444' }} />
                  <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                    4. Retrigger Payment (LOCKED)
                  </span>
                </div>
                <span className="badge badge-critical">SAFETY LOCK</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: '#fca5a5', lineHeight: 1.4 }}>
                STRICT SAFETY INVARIANT: Active bank debit confirmed with UTR. Blind retry is prohibited to prevent double charge.
              </div>
            </div>

            {/* Operator Notes */}
            <div style={{ marginTop: '8px' }}>
              <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '6px' }}>
                Investigator Remittance Note
              </div>
              <textarea
                placeholder="Enter justification for audit log (e.g., Verified UTR 414960264709 debit with bank switch; order cancelled at merchant OMS. Refund initiated.)"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                style={{
                  width: '100%',
                  height: '80px',
                  background: '#121212',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: '6px',
                  color: '#ffffff',
                  padding: '10px',
                  fontSize: '0.82rem',
                  outline: 'none',
                  resize: 'none'
                }}
              />
            </div>

            {/* Error Feedback */}
            {errorMessage && (
              <div style={{
                padding: '10px 14px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.35)',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                color: '#f87171',
                fontSize: '0.8rem',
                marginBottom: '10px'
              }}>
                <AlertTriangle size={16} style={{ flexShrink: 0 }} />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Submit */}
            <button
              onClick={handleExecute}
              disabled={resolving}
              className="btn btn-white"
              style={{ width: '100%', padding: '12px' }}
            >
              <ShieldCheck size={16} />
              {resolving ? 'Executing Remediation...' : `Execute ${selectedAction.replace(/_/g, ' ')}`}
            </button>
          </div>

          {/* Right Summary Sidebar */}
          <div style={{
            background: '#0a0a0a',
            border: '1px solid var(--border-subtle)',
            borderRadius: '12px',
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
            height: 'fit-content'
          }}>
            <div className="font-display" style={{ fontWeight: 700, fontSize: '0.9rem', color: '#ffffff' }}>
              Remediation Invariants
            </div>

            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
              • All financial state changes emit cryptographic audit events to MySQL.
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
              • Refunds trigger instant NPCI / VISA reversal batch instructions.
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
              • Operator ID: <span className="font-mono" style={{ color: '#ffffff' }}>{currentUser?.id || 'operator_priya_m'}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
