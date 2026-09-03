package com.paymentproof.service;

import com.paymentproof.dto.IncidentCaseDto;
import com.paymentproof.dto.PagedResponseDto;
import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.entity.enums.TriggerSource;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.IncidentCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentCaseRepository incidentCaseRepository;

    @InjectMocks
    private IncidentService incidentService;

    private IncidentCase mockIncident;

    @BeforeEach
    void setUp() {
        mockIncident = IncidentCase.builder()
                .incidentId("inc_test_200")
                .paymentId("pay_test_200")
                .incidentType(IncidentType.GATEWAY_SUCCESS_MISSING_WEBHOOK)
                .severity(Severity.HIGH)
                .caseStatus(CaseStatus.OPEN)
                .triggerSource(TriggerSource.WEBHOOK_MONITOR)
                .assignedInvestigator("operator_priya")
                .title("Missing webhook on captured payment")
                .description("Gateway captured INR 2500 but webhook was dropped.")
                .openedAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    @DisplayName("Retrieve paginated incidents with filter criteria")
    void testGetIncidentsWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<IncidentCase> page = new PageImpl<>(List.of(mockIncident), pageable, 1);

        when(incidentCaseRepository.findWithFilters(eq(CaseStatus.OPEN), eq(Severity.HIGH), eq(IncidentType.GATEWAY_SUCCESS_MISSING_WEBHOOK), any(), eq(pageable)))
                .thenReturn(page);

        PagedResponseDto<IncidentCaseDto> result = incidentService.getIncidents(
                CaseStatus.OPEN, Severity.HIGH, IncidentType.GATEWAY_SUCCESS_MISSING_WEBHOOK, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("inc_test_200", result.getContent().get(0).getIncidentId());
        assertEquals(Severity.HIGH, result.getContent().get(0).getSeverity());
    }

    @Test
    @DisplayName("Retrieve incident case by ID")
    void testGetIncidentById() {
        when(incidentCaseRepository.findById("inc_test_200")).thenReturn(Optional.of(mockIncident));

        IncidentCaseDto dto = incidentService.getIncidentById("inc_test_200");

        assertNotNull(dto);
        assertEquals("inc_test_200", dto.getIncidentId());
        assertEquals(IncidentType.GATEWAY_SUCCESS_MISSING_WEBHOOK, dto.getIncidentType());
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when incident not found")
    void testGetIncidentNotFound() {
        when(incidentCaseRepository.findById("non_existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.getIncidentById("non_existent"));
    }
}
