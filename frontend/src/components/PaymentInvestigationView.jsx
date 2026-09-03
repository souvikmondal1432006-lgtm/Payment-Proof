import React, { useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  ShieldAlert,
  ShieldCheck,
  Lock,
  ArrowRight,
  ChevronRight,
  Activity,
  Layers,
  Sparkles,
  Info,
  Terminal
} from 'lucide-react';
import PaymentTruthSection from './PaymentTruthSection';
import ConnectedEvidenceGraph from './ConnectedEvidenceGraph';
import ChronoEventTimeline from './ChronoEventTimeline';
import ContradictionMatrix from './ContradictionMatrix';
import EvidenceTraceDrawer from './EvidenceTraceDrawer';
import ErrorBanner from './ErrorBanner';
import LoadingSkeleton from './LoadingSkeleton';
import EmptyState from './EmptyState';

export default function PaymentInvestigationView({
  incident,
  onOpenResolution,
  onTriggerReanalysis,
  reanalyzing,
  loading,
  error,
  onRetry
}) {
  const [selectedSource, setSelectedSource] = useState(null);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [isEvidenceDrawerOpen, setIsEvidenceDrawerOpen] = useState(false);
  const [selectedQuestion, setSelectedQuestion] = useState('WHAT_HAPPENED');

  if (loading) {
    return (
      <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <LoadingSkeleton count={1} />
        <LoadingSkeleton type="stream" />
        <LoadingSkeleton count={2} />
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '24px' }}>
        <ErrorBanner
          error={error}
          onRetry={onRetry}
          title="Investigation Error"
        />
      </div>
    );
  }

  if (!incident) {
    return (
      <div style={{ padding: '40px', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <EmptyState
          title="No Payment Incident Selected"
          description="Select an incident from the queue to start authoritative investigation."
          icon="shield"
        />
      </div>
    );
  }

  const handleSelectSource = (srcId) => {
    setSelectedSource(srcId);
    setSelectedEvent(null);
    setIsEvidenceDrawerOpen(true);
  };

  const handleSelectEvent = (evt) => {
    setSelectedEvent(evt);
    setSelectedSource(evt.sourceId);
    setIsEvidenceDrawerOpen(true);
  };

  const handleSelectContradiction = (contra) => {
    setSelectedSource('BANK');
    setIsEvidenceDrawerOpen(true);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflowY: 'auto', padding: '24px', gap: '20px' }}>
      
      {/* ========================================================================= */}
      {/* 1. PROMINENT "PAYMENT TRUTH" COMMAND SECTION                              */}
      {/* Answers: What Happened, How Do You Know, Confidence, and What Should I Do */}
      {/* ========================================================================= */}
      <PaymentTruthSection
        incident={incident}
        onOpenResolution={onOpenResolution}
        onTriggerReanalysis={onTriggerReanalysis}
        onSelectEvidenceSource={handleSelectSource}
        selectedQuestion={selectedQuestion}
        onSelectQuestion={setSelectedQuestion}
      />

      {/* ========================================================================= */}
      {/* 2. 6 CONNECTED AUTHORITATIVE EVIDENCE SOURCES GRAPH                       */}
      {/* Bank, Gateway, Merchant, Webhook, Settlement, Refund                      */}
      {/* ========================================================================= */}
      <ConnectedEvidenceGraph
        incident={incident}
        selectedSource={selectedSource}
        onSelectSource={handleSelectSource}
      />

      {/* ========================================================================= */}
      {/* 3. SUBSYSTEM CONTRADICTION & CLASH ANALYSIS                               */}
      {/* ========================================================================= */}
      <ContradictionMatrix
        incident={incident}
        onSelectContradiction={handleSelectContradiction}
      />

      {/* ========================================================================= */}
      {/* 4. FORENSIC CHRONOLOGICAL TIMELINE (MILLSECOND EVENT TRACE)               */}
      {/* ========================================================================= */}
      <ChronoEventTimeline
        incident={incident}
        onSelectEvent={handleSelectEvent}
        selectedEventId={selectedEvent?.id}
      />

      {/* ========================================================================= */}
      {/* 5. DEEP EVIDENCE TRACE DRAWER (RAW ISO-8583 / JSON / SHA-256 CHECKSUM)    */}
      {/* ========================================================================= */}
      <EvidenceTraceDrawer
        isOpen={isEvidenceDrawerOpen}
        onClose={() => setIsEvidenceDrawerOpen(false)}
        selectedSource={selectedSource}
        selectedEvent={selectedEvent}
        incident={incident}
      />

    </div>
  );
}
