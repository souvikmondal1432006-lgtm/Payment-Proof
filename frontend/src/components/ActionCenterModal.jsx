import React, { useState } from 'react';
import { X, Lock, CheckCircle2, AlertTriangle, ShieldCheck, ArrowRight, Check } from 'lucide-react';
import { api } from '../services/api';

export default function ActionCenterModal({
  caseData,
  incident,
  isOpen,
  onClose,
  onResolve,
  onResolved,
  resolving: externalResolving,
  currentUser
}) {
  const activeCase = incident || caseData;
  if (!isOpen || !activeCase) return null;

  const [selectedAction, setSelectedAction] = useState('AUTO_REFUND_CUSTOMER');
  const [operatorId, setOperatorId] = useState(currentUser?.name || currentUser?.id || 'Priya Mukherjee');
  const [notes, setNotes] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [internalResolving, setInternalResolving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const isBankDebited = activeCase.bank?.status === 'SUCCESS' || activeCase.bank?.status === 'DEBITED';
  const isResolving = externalResolving || internalResolving;

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

  const resolutionOptions = [
    {
      value: 'AUTO_REFUND_CUSTOMER',
      label: 'Auto-Refund Customer (Safest Next Action)',
      desc: `Authoritatively issue instant bank reversal of ₹${Number(activeCase.amount || 0).toLocaleString('en-IN')} to original payment source.`,
      badge: 'RECOMMENDED',
      isLocked: false
    },
    {
      value: 'RESEND_WEBHOOK',
      label: 'Re-Dispatch Authoritative Webhook',
      desc: `Generate signed HMAC webhook and trigger retry dispatch to merchant OMS.`,
      badge: 'MERCHANT REPLAY',
      isLocked: false
    },
    {
      value: 'FORCE_SETTLE_MERCHANT',
      label: 'Force-Settle Merchant Ledger',
      desc: 'Reconcile transaction to SUCCESS. Acknowledges late bank capture and credits merchant ledger.',
      badge: 'SETTLEMENT OVERRIDE',
      isLocked: false
    },
    {
      value: 'MANUAL_BANK_ESCALATION',
      label: 'Escalate to Partner Bank Nodal Desk',
      desc: 'Flag transaction as DISPUTED. Generate settlement discrepancy packet for manual bank operations.',
      badge: 'ESCALATION',
      isLocked: false
    },
    {
      value: 'RETRIGGER_PAYMENT',
      label: 'Retrigger Payment (LOCKED)',
      desc: 'STRICT SAFETY INVARIANT: Active bank debit confirmed. Blind retry is strictly prohibited to prevent duplicate debit.',
      badge: 'SAFETY LOCKOUT',
      isLocked: isBankDebited
    }
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!confirmed || isResolving) return;

    setErrorMessage(null);
    setInternalResolving(true);
    try {
      const targetId = activeCase.incidentId || activeCase.id || activeCase.paymentId;
      const mappedAction = ACTION_MAPPING[selectedAction] || selectedAction;
      const payload = {
        actionTaken: mappedAction,
        resolutionType: 'OPERATOR_MANUAL_OVERRIDE',
        resolvedBy: operatorId || 'Priya Mukherjee',
        resolutionNotes: notes || `Authoritative remediation executed for incident ${targetId}.`,
        notes: notes || `Authoritative remediation executed for incident ${targetId}.`,
        financialImpactAmount: Number(activeCase.amount || 0),
        liabilityParty: 'PLATFORM_LOSS_NONE'
      };

      if (onResolve) {
        await onResolve(payload);
      } else {
        await api.resolveIncident(targetId, payload);
      }

      setSuccess(true);
      setTimeout(() => {
        if (onResolved) onResolved();
        onClose();
      }, 1200);
    } catch (err) {
      console.error('Failed to execute resolution:', err);
      setErrorMessage(err.humanMessage || err.message || 'Failed to execute authoritative resolution.');
    } finally {
      setInternalResolving(false);
    }
  };

  return (
    <div
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0, 0, 0, 0.85)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '16px',
        overflowY: 'auto'
      }}
    >
      <div className="surface-card animate-fade-in" style={{
        width: '100%',
        maxWidth: '560px',
        maxHeight: 'calc(100vh - 32px)',
        display: 'flex',
        flexDirection: 'column',
        background: '#0a0a0a',
        border: '1px solid var(--border-strong)',
        boxShadow: '0 20px 60px rgba(0,0,0,0.9)',
        borderRadius: '14px',
        overflow: 'hidden'
      }}>
        {/* Header - Fixed */}
        <div style={{
          padding: '14px 18px',
          borderBottom: '1px solid var(--border-subtle)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexShrink: 0,
          background: '#080808'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '32px',
              height: '32px',
              borderRadius: '8px',
              background: '#ffffff',
              color: '#000000',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              <ShieldCheck size={18} />
            </div>
            <div>
              <h2 className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, color: '#ffffff', lineHeight: 1.2 }}>
                Authoritative Incident Resolution
              </h2>
              <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                {activeCase.paymentId} • ₹{Number(activeCase.amount || 0).toLocaleString('en-IN')}
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              padding: '6px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              borderRadius: '6px'
            }}
            title="Close"
          >
            <X size={18} />
          </button>
        </div>

        {success ? (
          <div style={{ padding: '36px', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
            <div style={{
              width: '44px',
              height: '44px',
              borderRadius: '50%',
              background: '#10b981',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#000000'
            }}>
              <Check size={26} strokeWidth={3} />
            </div>
            <div className="font-display" style={{ fontSize: '1.2rem', fontWeight: 800, color: '#ffffff' }}>
              Resolution Executed Successfully
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Action <span className="font-mono" style={{ color: '#10b981', fontWeight: 700 }}>{selectedAction}</span> recorded to audit ledger.
            </div>
          </div>
        ) : (
          /* Scrollable Form Body */
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
            <div style={{ padding: '16px 18px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
              
              {/* Action Choices */}
              <div>
                <label style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '8px', display: 'block' }}>
                  Select Reconciled Authoritative Action:
                </label>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {resolutionOptions.map(opt => {
                    const isSelected = selectedAction === opt.value;
                    const isLocked = opt.isLocked;

                    return (
                      <div
                        key={opt.value}
                        onClick={() => !isLocked && setSelectedAction(opt.value)}
                        style={{
                          padding: '10px 12px',
                          background: isLocked ? 'rgba(239, 68, 68, 0.05)' : isSelected ? 'rgba(255, 255, 255, 0.08)' : '#050505',
                          border: `1px solid ${isLocked ? 'rgba(239, 68, 68, 0.25)' : isSelected ? '#ffffff' : 'var(--border-subtle)'}`,
                          borderRadius: '8px',
                          cursor: isLocked ? 'not-allowed' : 'pointer',
                          opacity: isLocked ? 0.65 : 1,
                          transition: 'all 0.15s ease'
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            {isLocked && <Lock size={12} style={{ color: '#ef4444' }} />}
                            <div className="font-display" style={{ fontSize: '0.84rem', fontWeight: 700, color: isLocked ? '#fca5a5' : '#ffffff' }}>
                              {opt.label}
                            </div>
                          </div>
                          <span className={`badge ${isLocked ? 'badge-critical' : opt.badge === 'RECOMMENDED' ? 'badge-success' : 'badge-outline'}`} style={{ fontSize: '0.62rem', padding: '2px 6px' }}>
                            {opt.badge}
                          </span>
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', lineHeight: 1.35 }}>
                          {opt.desc}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Operator & Notes */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px', display: 'block' }}>
                    Executing Operator
                  </label>
                  <input
                    type="text"
                    value={operatorId}
                    onChange={(e) => setOperatorId(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '7px 10px',
                      background: '#000000',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: '6px',
                      color: '#ffffff',
                      fontSize: '0.8rem'
                    }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px', display: 'block' }}>
                    Audit Notes
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Bank UTR verified"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '7px 10px',
                      background: '#000000',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: '6px',
                      color: '#ffffff',
                      fontSize: '0.8rem'
                    }}
                  />
                </div>
              </div>

              {/* Confirmation Checkbox */}
              <div style={{
                padding: '10px 12px',
                background: '#040404',
                border: '1px solid var(--border-subtle)',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <input
                  type="checkbox"
                  id="confirmResolution"
                  checked={confirmed}
                  onChange={(e) => setConfirmed(e.target.checked)}
                  style={{ width: '15px', height: '15px', accentColor: '#ffffff', cursor: 'pointer', flexShrink: 0 }}
                />
                <label htmlFor="confirmResolution" style={{ fontSize: '0.74rem', color: '#f4f4f5', cursor: 'pointer' }}>
                  I confirm authoritative state mutation under immutable audit logging.
                </label>
              </div>
            </div>

            {/* Error Feedback */}
            {errorMessage && (
              <div style={{
                margin: '0 18px 12px',
                padding: '10px 14px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.35)',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                color: '#f87171',
                fontSize: '0.78rem'
              }}>
                <AlertTriangle size={16} style={{ flexShrink: 0 }} />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Footer - Fixed Bottom */}
            <div style={{
              padding: '12px 18px',
              borderTop: '1px solid var(--border-subtle)',
              background: '#080808',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '10px',
              flexShrink: 0
            }}>
              <button
                type="button"
                onClick={onClose}
                className="btn btn-outline-white btn-sm"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!confirmed || isResolving}
                className="btn btn-white btn-sm"
                style={{ padding: '7px 16px' }}
              >
                {isResolving ? 'Executing...' : 'Execute Remediation'} <ArrowRight size={14} />
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
