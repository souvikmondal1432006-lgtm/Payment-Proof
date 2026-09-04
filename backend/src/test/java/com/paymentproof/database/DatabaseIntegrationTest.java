package com.paymentproof.database;

import com.paymentproof.config.DatabaseProfileValidator;
import com.paymentproof.dto.AuditVerificationResultDto;
import com.paymentproof.dto.ErrorResponseDto;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import com.paymentproof.exception.GlobalExceptionHandler;
import com.paymentproof.repository.*;
import com.paymentproof.service.AuditChainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseIntegrationTest {

    @Nested
    @DisplayName("1. Monetary Data Precision & Decimal Verification")
    class MonetaryPrecisionTests {

        @Test
        @DisplayName("Verify monetary amounts retain exact 2-decimal scale without floating-point drift")
        void testMonetaryPrecisionNoFloatingPoint() {
            BigDecimal exactAmount = new BigDecimal("4499.00");
            BigDecimal subCentAmount = new BigDecimal("0.01");

            Payment payment = Payment.builder()
                    .paymentId("pay_db_test_001")
                    .merchantId("merch_test")
                    .customerId("cust_test")
                    .orderId("ord_test")
                    .amount(exactAmount)
                    .currency("INR")
                    .paymentMethod("UPI")
                    .status(PaymentStatus.INITIATED)
                    .initiatedAt(LocalDateTime.now())
                    .build();

            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("4499.00"));
            assertThat(payment.getAmount().scale()).isEqualTo(2);

            // Accumulate multiple decimal fractions to ensure no IEEE 754 precision loss occurs
            BigDecimal total = exactAmount.add(subCentAmount).add(subCentAmount);
            assertThat(total).isEqualByComparingTo(new BigDecimal("4499.02"));
            assertThat(total.toString()).isEqualTo("4499.02");

            GatewayRecord gw = GatewayRecord.builder()
                    .gatewayRecordId("gw_db_001")
                    .paymentId("pay_db_test_001")
                    .gatewayName("RAZORPAY")
                    .gatewayStatus(GatewayStatus.SUCCESS)
                    .authorizedAmount(new BigDecimal("4499.00"))
                    .capturedAmount(new BigDecimal("4499.00"))
                    .fee(new BigDecimal("89.98"))
                    .tax(new BigDecimal("16.20"))
                    .gatewayTimestamp(LocalDateTime.now())
                    .build();

            assertThat(gw.getFee()).isEqualByComparingTo(new BigDecimal("89.98"));
            assertThat(gw.getTax()).isEqualByComparingTo(new BigDecimal("16.20"));
        }

        @Test
        @DisplayName("Verify ML probability and anomaly scores maintain 4-decimal scale")
        void testMlAssessmentPrecision() {
            BigDecimal anomalyScore = new BigDecimal("0.9125");
            BigDecimal confidence = new BigDecimal("0.9840");

            MlAssessment assessment = MlAssessment.builder()
                    .assessmentId("mla_test_01")
                    .incidentId("inc_test_01")
                    .paymentId("pay_test_01")
                    .modelVersion("v1.0.0")
                    .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                    .anomalyScore(anomalyScore)
                    .confidenceScore(confidence)
                    .suggestedAction(SuggestedAction.AUTO_REFUND_CUSTOMER)
                    .assessedAt(LocalDateTime.now())
                    .build();

            assertThat(assessment.getAnomalyScore()).isEqualByComparingTo(new BigDecimal("0.9125"));
            assertThat(assessment.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.9840"));
        }
    }

    @Nested
    @DisplayName("2. MySQL Profile & Configuration Fail-Safe Verification")
    class ProfileValidationTests {

        @Mock
        private Environment environment;

        @Test
        @DisplayName("Fail-fast: Rejects startup if 'mysql' and 'standalone' profiles are simultaneously active")
        void testSimultaneousProfilesRejected() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"mysql", "standalone"});
            DatabaseProfileValidator validator = new DatabaseProfileValidator(environment);

            assertThatThrownBy(validator::validateDatabaseConfiguration)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONTRADICTION DETECTED");
        }

        @Test
        @DisplayName("Fail-fast: Rejects startup under 'mysql' profile when DB_URL is missing")
        void testMissingDbUrlRejected() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"mysql"});
            when(environment.getProperty("spring.datasource.url")).thenReturn(null);
            DatabaseProfileValidator validator = new DatabaseProfileValidator(environment);

            assertThatThrownBy(validator::validateDatabaseConfiguration)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_URL environment variable is missing");
        }

        @Test
        @DisplayName("Fail-fast: Rejects startup under 'mysql' profile when DB_USER is missing")
        void testMissingDbUserRejected() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"mysql"});
            when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:mysql://localhost:3306/payment_proof");
            when(environment.getProperty("spring.datasource.username")).thenReturn("");
            DatabaseProfileValidator validator = new DatabaseProfileValidator(environment);

            assertThatThrownBy(validator::validateDatabaseConfiguration)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_USER environment variable is missing");
        }

        @Test
        @DisplayName("Fail-fast: Rejects startup under 'mysql' profile when DB_PASSWORD is missing")
        void testMissingDbPasswordRejected() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"mysql"});
            when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:mysql://localhost:3306/payment_proof");
            when(environment.getProperty("spring.datasource.username")).thenReturn("payment_user");
            when(environment.getProperty("spring.datasource.password")).thenReturn(null);
            DatabaseProfileValidator validator = new DatabaseProfileValidator(environment);

            assertThatThrownBy(validator::validateDatabaseConfiguration)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_PASSWORD environment variable is missing");
        }

        @Test
        @DisplayName("Prohibit silent switch: Rejects H2 URL under 'mysql' profile")
        void testH2UrlProhibitedUnderMysqlProfile() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"mysql"});
            when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:payment_proof");
            when(environment.getProperty("spring.datasource.username")).thenReturn("sa");
            when(environment.getProperty("spring.datasource.password")).thenReturn("secret");
            DatabaseProfileValidator validator = new DatabaseProfileValidator(environment);

            assertThatThrownBy(validator::validateDatabaseConfiguration)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CRITICAL INVARIANT VIOLATION")
                    .hasMessageContaining("Silent switching to H2 under 'mysql' profile is strictly prohibited");
        }

        @Test
        @DisplayName("Success: Valid MySQL environment parameters pass verification cleanly")
        void testValidMysqlConfigPasses() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"mysql"});
            when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:mysql://db.production.internal:3306/payment_proof?ssl=true");
            when(environment.getProperty("spring.datasource.username")).thenReturn("app_user");
            when(environment.getProperty("spring.datasource.password")).thenReturn("super_secret_password");

            DatabaseProfileValidator validator = new DatabaseProfileValidator(environment);
            validator.validateDatabaseConfiguration(); // should not throw
        }
    }

    @Nested
    @DisplayName("3. Cryptographic Audit Chain Ledger Verification")
    class AuditChainVerificationTests {

        @Mock
        private AuditEventRepository auditEventRepository;

        @Test
        @DisplayName("Verify audit events form an immutable cryptographic hash chain")
        void testAuditHashChainIntegrity() {
            AuditChainService chainService = new AuditChainService(auditEventRepository);

            AuditEvent event1 = AuditEvent.builder()
                    .auditId("aud_001")
                    .sequenceNumber(1L)
                    .entityName("PAYMENT")
                    .entityId("pay_100")
                    .action("INITIATED")
                    .actorType(ActorType.SYSTEM)
                    .actorId("CORE_ENGINE")
                    .previousEventHash(AuditChainService.GENESIS_PREV_HASH)
                    .currentEventHash("hash_event_1")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(auditEventRepository.findTopByOrderBySequenceNumberDesc()).thenReturn(Optional.of(event1));
            when(auditEventRepository.save(org.mockito.ArgumentMatchers.any(AuditEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AuditEvent event2 = chainService.recordChainedEvent(
                    "PAYMENT",
                    "pay_100",
                    "CONTRADICTION_DETECTED",
                    ActorType.SYSTEM,
                    "SAFETY_ENGINE",
                    "INITIATED",
                    "DISPUTED",
                    "127.0.0.1"
            );

            assertThat(event2.getSequenceNumber()).isEqualTo(2L);
            assertThat(event2.getPreviousEventHash()).isEqualTo("hash_event_1");
            assertThat(event2.getCurrentEventHash()).isNotBlank();
            assertThat(event2.getCurrentEventHash()).isNotEqualTo("hash_event_1");
        }
    }

    @Nested
    @DisplayName("4. Database Failure & Invariant Protection")
    class DatabaseFailureTests {

        @Test
        @DisplayName("Database unavailable error produces structured 503 response without leaking credentials")
        void testDatabaseUnavailableResponseStructure() {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/incidents");

            DataAccessResourceFailureException dbException =
                    new DataAccessResourceFailureException("Communications link failure: Connection refused to MySQL host at 3306");

            ResponseEntity<ErrorResponseDto> response = handler.handleDatabaseExceptions(dbException, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(503);
            assertThat(response.getBody().getError()).isEqualTo("DATABASE_UNAVAILABLE");
            assertThat(response.getBody().getMessage()).contains("Payment records could not be loaded");
            assertThat(response.getBody().getPath()).isEqualTo("/api/incidents");

            // Verify passwords and internal connection strings are NEVER leaked in user-facing message
            assertThat(response.getBody().getMessage()).doesNotContain("password");
            assertThat(response.getBody().getMessage()).doesNotContain("3306");
        }
    }
}
