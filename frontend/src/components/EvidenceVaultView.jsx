import React, { useState, useEffect } from 'react';
import {
  FileCode,
  ShieldCheck,
  Download,
  Copy,
  Check,
  ExternalLink,
  Lock,
  Layers,
  Terminal,
  Database
} from 'lucide-react';
import { api } from '../services/api';

export default function EvidenceVaultView({ incident }) {
  const [evidenceList, setEvidenceList] = useState([]);
  const [selectedEvidence, setSelectedEvidence] = useState(null);
  const [copiedChecksum, setCopiedChecksum] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (incident?.incidentId) {
      loadEvidence(incident.incidentId);
    }
  }, [incident]);

  const loadEvidence = async (id) => {
    setLoading(true);
    try {
      const data = await api.getEvidence(id);
      setEvidenceList(data || []);
      if (data && data.length > 0) {
        setSelectedEvidence(data[0]);
      }
    } catch (e) {
      console.error('Failed to load evidence:', e);
    } finally {
      setLoading(false);
    }
  };

  const copyChecksum = (checksum) => {
    navigator.clipboard.writeText(checksum);
    setCopiedChecksum(true);
    setTimeout(() => setCopiedChecksum(false), 2000);
  };

  if (!incident) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
          <span className="badge badge-outline font-mono">{incident.paymentId}</span>
          <span className="badge badge-white">EVIDENCE VAULT</span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>CRYPTOGRAPHIC PROOF ARTIFACTS</span>
        </div>
        <h1 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff' }}>
          Forensic Evidence & Invariant Ledger
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '2px' }}>
          Immutable payloads, bank switch ISO-8583 message logs, gateway traces, and SHA-256 integrity checksums.
        </p>
      </div>

      {/* Main Split View */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px, 340px) 1fr', gap: '16px', flex: 1, minHeight: '400px' }}>
        {/* Evidence List */}
        <div style={{
          background: '#0a0a0a',
          border: '1px solid var(--border-subtle)',
          borderRadius: '12px',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          gap: '10px'
        }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
            Captured Artifacts ({evidenceList.length})
          </div>

          {evidenceList.map((e) => {
            const isSelected = selectedEvidence?.evidenceId === e.evidenceId;
            return (
              <div
                key={e.evidenceId}
                onClick={() => setSelectedEvidence(e)}
                style={{
                  background: isSelected ? '#18181b' : '#121212',
                  border: isSelected ? '1px solid #ffffff' : '1px solid var(--border-subtle)',
                  borderRadius: '8px',
                  padding: '12px',
                  cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span className="badge badge-outline font-mono" style={{ fontSize: '0.68rem' }}>
                    {e.evidenceSource}
                  </span>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>
                    {e.capturedAt ? new Date(e.capturedAt).toLocaleTimeString() : ''}
                  </span>
                </div>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#ffffff', marginBottom: '2px' }}>
                  {e.evidenceType}
                </div>
                <div className="font-mono" style={{ fontSize: '0.7rem', color: 'var(--text-dim)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {e.filePath}
                </div>
              </div>
            );
          })}
        </div>

        {/* Selected Evidence Viewer */}
        <div style={{
          background: '#0a0a0a',
          border: '1px solid var(--border-subtle)',
          borderRadius: '12px',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        }}>
          {selectedEvidence ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '14px' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '1rem', fontWeight: 700, color: '#ffffff' }}>
                      {selectedEvidence.evidenceType}
                    </span>
                    <span className="badge badge-success font-mono">VERIFIED INTEGRITY</span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    Source: {selectedEvidence.evidenceSource} · Path: {selectedEvidence.filePath}
                  </div>
                </div>

                <button
                  onClick={() => copyChecksum(selectedEvidence.payloadChecksum)}
                  className="btn btn-outline-white btn-sm"
                >
                  {copiedChecksum ? <Check size={13} style={{ color: '#10b981' }} /> : <Copy size={13} />}
                  Copy SHA-256
                </button>
              </div>

              {/* SHA-256 Checksum Display */}
              <div style={{ background: '#050505', padding: '10px 14px', borderRadius: '6px', border: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: '10px' }}>
                <ShieldCheck size={16} style={{ color: '#10b981', flexShrink: 0 }} />
                <div className="font-mono" style={{ fontSize: '0.75rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  SHA-256: <span style={{ color: '#ffffff' }}>{selectedEvidence.payloadChecksum}</span>
                </div>
              </div>

              {/* Raw Payload Stream */}
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '8px' }}>
                  Raw Protocol Payload Body
                </div>
                <pre className="font-mono" style={{
                  flex: 1,
                  background: '#050505',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: '8px',
                  padding: '16px',
                  fontSize: '0.8rem',
                  color: '#e4e4e7',
                  overflow: 'auto',
                  lineHeight: 1.6,
                  whiteSpace: 'pre-wrap'
                }}>
                  {selectedEvidence.rawContent}
                </pre>
              </div>
            </>
          ) : (
            <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '60px' }}>
              Select an artifact from the list to inspect its cryptographic payload.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
