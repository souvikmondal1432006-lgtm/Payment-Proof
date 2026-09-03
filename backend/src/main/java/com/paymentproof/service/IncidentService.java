package com.paymentproof.service;

import com.paymentproof.dto.IncidentCaseDto;
import com.paymentproof.dto.PagedResponseDto;
import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.IncidentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentCaseRepository incidentCaseRepository;

    @Transactional(readOnly = true)
    public PagedResponseDto<IncidentCaseDto> getIncidents(
            CaseStatus caseStatus,
            Severity severity,
            IncidentType incidentType,
            String search,
            Pageable pageable) {

        Page<IncidentCase> page = incidentCaseRepository.findWithFilters(
                caseStatus,
                severity,
                incidentType,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );

        List<IncidentCaseDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponseDto.<IncidentCaseDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public IncidentCaseDto getIncidentById(String incidentId) {
        IncidentCase incident = incidentCaseRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCase", "incidentId", incidentId));
        return mapToDto(incident);
    }

    public IncidentCaseDto mapToDto(IncidentCase i) {
        if (i == null) return null;
        return IncidentCaseDto.builder()
                .incidentId(i.getIncidentId())
                .paymentId(i.getPaymentId())
                .incidentType(i.getIncidentType())
                .severity(i.getSeverity())
                .caseStatus(i.getCaseStatus())
                .triggerSource(i.getTriggerSource())
                .assignedInvestigator(i.getAssignedInvestigator())
                .title(i.getTitle())
                .description(i.getDescription())
                .openedAt(i.getOpenedAt())
                .resolvedAt(i.getResolvedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
