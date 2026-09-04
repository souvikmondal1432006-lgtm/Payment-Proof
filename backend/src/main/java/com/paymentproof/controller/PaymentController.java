package com.paymentproof.controller;

import com.paymentproof.dto.PagedResponseDto;
import com.paymentproof.dto.PaymentDetailDto;
import com.paymentproof.dto.PaymentDto;
import com.paymentproof.dto.TimelineEventDto;
import com.paymentproof.entity.enums.PaymentStatus;
import com.paymentproof.service.PaymentService;
import com.paymentproof.service.TimelineService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TimelineService timelineService;

    @GetMapping
    public ResponseEntity<PagedResponseDto<PaymentDto>> getPayments(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "initiatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponseDto<PaymentDto> response = paymentService.getPayments(merchantId, status, paymentMethod, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailDto> getPaymentById(@PathVariable("id") String paymentId) {
        log.info("Fetching payment details for ID: {}", paymentId);
        PaymentDetailDto paymentDetail = paymentService.getPaymentDetail(paymentId);
        return ResponseEntity.ok(paymentDetail);
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineEventDto>> getPaymentTimeline(@PathVariable("id") String paymentId) {
        log.info("Fetching unified chronological timeline for payment: {}", paymentId);
        List<TimelineEventDto> timeline = timelineService.getTimelineForPayment(paymentId);
        return ResponseEntity.ok(timeline);
    }
}
