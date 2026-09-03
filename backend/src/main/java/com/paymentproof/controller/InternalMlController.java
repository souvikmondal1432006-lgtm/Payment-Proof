package com.paymentproof.controller;

import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.dto.MlFeatureRequestDto;
import com.paymentproof.service.MlClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/ml")
@RequiredArgsConstructor
public class InternalMlController {

    private final MlClassificationService mlClassificationService;

    @PostMapping("/classify")
    public ResponseEntity<MlAssessmentDto> classify(@Valid @RequestBody MlFeatureRequestDto request) {
        log.info("Internal ML Classification invoked for payment: {}", request.getPaymentId());
        MlAssessmentDto assessment = mlClassificationService.classify(request);
        return ResponseEntity.ok(assessment);
    }
}
