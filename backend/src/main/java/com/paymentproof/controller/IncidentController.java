package com.paymentproof.controller;

import com.paymentproof.dto.*;
import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.service.EvidenceService;
import com.paymentproof.service.IncidentService;
import com.paymentproof.service.InvestigationService;
import com.paymentproof.service.ResolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final InvestigationService investigationService;
    private final EvidenceService evidenceService;
    private final ResolutionService resolutionService;

    @GetMapping
    public ResponseEntity<PagedResponseDto<IncidentCaseDto>> getIncidents(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "openedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponseDto<IncidentCaseDto> response = incidentService.getIncidents(status, severity, type, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentCaseDto> getIncidentById(@PathVariable("id") String incidentId) {
        log.info("Fetching incident case for ID: {}", incidentId);
        IncidentCaseDto incident = incidentService.getIncidentById(incidentId);
        return ResponseEntity.ok(incident);
    }

    @PostMapping("/{id}/investigate")
    public ResponseEntity<InvestigationResultDto> investigateIncident(@PathVariable("id") String incidentId) {
        log.info("Triggering forensic investigation for incident ID: {}", incidentId);
        InvestigationResultDto result = investigationService.investigateIncident(incidentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/ai-report")
    public ResponseEntity<AiInvestigationReportDto> getAiInvestigationReport(@PathVariable("id") String incidentId) {
        log.info("Fetching structured AI investigation report for incident ID: {}", incidentId);
        AiInvestigationReportDto report = investigationService.getAiReportForIncident(incidentId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<List<InvestigationEvidenceDto>> getIncidentEvidence(@PathVariable("id") String incidentId) {
        log.info("Fetching investigation evidence for incident ID: {}", incidentId);
        List<InvestigationEvidenceDto> evidence = evidenceService.getEvidenceForIncident(incidentId);
        return ResponseEntity.ok(evidence);
    }

    @GetMapping("/{id}/resolution")
    public ResponseEntity<ResolutionDto> getIncidentResolution(@PathVariable("id") String incidentId) {
        log.info("Fetching authoritative resolution for incident ID: {}", incidentId);
        ResolutionDto resolution = resolutionService.getResolutionForIncident(incidentId);
        return ResponseEntity.ok(resolution);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ResolutionDto> resolveIncident(
            @PathVariable("id") String incidentId,
            @Valid @RequestBody ResolutionRequestDto request) {
        log.info("Resolving incident ID: {} with action: {}", incidentId, request.getActionTaken());
        ResolutionDto resolution = resolutionService.resolveIncident(incidentId, request);
        return ResponseEntity.ok(resolution);
    }
}
