package com.paymentproof.service;

import com.paymentproof.dto.InvestigationEvidenceDto;
import com.paymentproof.entity.InvestigationEvidence;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.IncidentCaseRepository;
import com.paymentproof.repository.InvestigationEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final InvestigationEvidenceRepository evidenceRepository;
    private final IncidentCaseRepository incidentCaseRepository;

    @Transactional(readOnly = true)
    public List<InvestigationEvidenceDto> getEvidenceForIncident(String incidentId) {
        if (!incidentCaseRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("IncidentCase", "incidentId", incidentId);
        }

        List<InvestigationEvidence> evidence = evidenceRepository.findByIncidentId(incidentId);
        return evidence.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvestigationEvidenceDto> getEvidenceForPayment(String paymentId) {
        List<InvestigationEvidence> evidence = evidenceRepository.findByPaymentId(paymentId);
        return evidence.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public InvestigationEvidenceDto mapToDto(InvestigationEvidence e) {
        if (e == null) return null;
        return InvestigationEvidenceDto.builder()
                .evidenceId(e.getEvidenceId())
                .incidentId(e.getIncidentId())
                .paymentId(e.getPaymentId())
                .evidenceSource(e.getEvidenceSource())
                .evidenceType(e.getEvidenceType())
                .filePath(e.getFilePath())
                .rawContent(e.getRawContent())
                .payloadChecksum(e.getPayloadChecksum())
                .capturedAt(e.getCapturedAt())
                .build();
    }
}
