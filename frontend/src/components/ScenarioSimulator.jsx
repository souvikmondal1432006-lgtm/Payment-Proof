import React, { useState } from 'react';
import { X, PlayCircle, ShieldAlert, Zap, ArrowRight, Check } from 'lucide-react';

export default function ScenarioSimulator({
  isOpen,
  onClose,
  onSimulate,
  onSimulationComplete,
  simulating: externalSimulating
}) {
  if (!isOpen) return null;

  const [scenarioType, setScenarioType] = useState('GHOST_CAPTURE');
  const [merchantName, setMerchantName] = useState('BookMyShow India');
  const [amount, setAmount] = useState('8500.00');
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [internalSimulating, setInternalSimulating] = useState(false);
  const [success, setSuccess] = useState(false);

  const scenarios = [
    {
      type: 'GHOST_CAPTURE',
      title: 'Ghost Debit (Bank Debited, Gateway Timeout, Order Cancelled)',
      desc: 'Bank switch reports SUCCESS (00), Gateway reports 65s timeout, Merchant OMS auto-cancels the seat reservation.',
      defaultAmount: '8500.00',
      defaultMerchant: 'BookMyShow India',
      badge: 'CRITICAL'
    },
    {
      type: 'WEBHOOK_DROPPED',
      title: 'Dropped Webhook (Captured but Unfulfilled)',
      desc: 'Payment captured at Bank and Gateway, but asynchronous webhook dropped after 3 HTTP 504 attempts.',
      defaultAmount: '1450.00',
      defaultMerchant: 'Swiggy Food Delivery',
      badge: 'HIGH'
    },
    {
      type: 'EXTREME_3DS_LATENCY',
      title: 'Extreme 3DS Authorization Latency (48.5s)',
      desc: 'Customer experiences 48-second delay on OTP screen; late capture clears after browser session times out.',
      defaultAmount: '34999.00',
      defaultMerchant: 'Flipkart Electronics',
      badge: 'MEDIUM'
    },
    {
      type: 'DOUBLE_DEBIT',
      title: 'Double Debit / Duplicate Charge',
      desc: 'Customer retried payment on slow network, resulting in 2 distinct bank debits for single cart.',
      defaultAmount: '3200.00',
      defaultMerchant: 'Zomato Dining',
      badge: 'CRITICAL'
    }
  ];

  const handleSelectScenario = (s) => {
    setScenarioType(s.type);
    setAmount(s.defaultAmount);
    setMerchantName(s.defaultMerchant);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setInternalSimulating(true);

    const payload = {
      scenarioType,
      merchantName,
      amount: parseFloat(amount) || 1000.0,
      paymentMethod
    };

    setTimeout(() => {
      setInternalSimulating(false);
      setSuccess(true);
      setTimeout(() => {
        if (onSimulationComplete) onSimulationComplete(payload);
        if (onSimulate) onSimulate(payload);
        onClose();
      }, 1000);
    }, 800);
  };

  const isSimulating = externalSimulating || internalSimulating;

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
              background: '#f59e0b',
              color: '#000000',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              <Zap size={18} />
            </div>
            <div>
              <h2 className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, color: '#ffffff', lineHeight: 1.2 }}>
                Payment Anomaly Simulator
              </h2>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                Inject multi-system inconsistency telemetry
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
              Anomaly Telemetry Injected
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Telemetry dispatched to ML engine and investigation queue updated.
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
            <div style={{ padding: '16px 18px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
              {/* Scenario Selection */}
              <div>
                <label style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '8px', display: 'block' }}>
                  Select Anomaly Scenario:
                </label>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {scenarios.map(s => {
                    const isSelected = scenarioType === s.type;
                    return (
                      <div
                        key={s.type}
                        onClick={() => handleSelectScenario(s)}
                        style={{
                          padding: '10px 12px',
                          background: isSelected ? 'rgba(245, 158, 11, 0.1)' : '#050505',
                          border: `1px solid ${isSelected ? '#f59e0b' : 'var(--border-subtle)'}`,
                          borderRadius: '8px',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease'
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2px' }}>
                          <div className="font-display" style={{ fontSize: '0.84rem', fontWeight: 700, color: isSelected ? '#fbbf24' : '#ffffff' }}>
                            {s.title}
                          </div>
                          <span className={`badge ${s.badge === 'CRITICAL' ? 'badge-critical' : 'badge-high'}`} style={{ fontSize: '0.62rem', padding: '2px 6px' }}>
                            {s.badge}
                          </span>
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', lineHeight: 1.35 }}>
                          {s.desc}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Custom Parameters */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px', display: 'block' }}>
                    Target Merchant
                  </label>
                  <input
                    type="text"
                    value={merchantName}
                    onChange={(e) => setMerchantName(e.target.value)}
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
                    Simulated Amount (INR)
                  </label>
                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
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
            </div>

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
                disabled={isSimulating}
                className="btn btn-white btn-sm"
                style={{ padding: '7px 16px' }}
              >
                {isSimulating ? 'Injecting Telemetry...' : 'Inject & Investigate'} <ArrowRight size={14} />
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
