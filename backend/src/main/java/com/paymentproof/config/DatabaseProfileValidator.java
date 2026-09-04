package com.paymentproof.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseProfileValidator {

    private final Environment environment;

    @PostConstruct
    public void validateDatabaseConfiguration() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());

        boolean isMysqlActive = activeProfiles.contains("mysql");
        boolean isStandaloneActive = activeProfiles.contains("standalone");

        if (isMysqlActive && isStandaloneActive) {
            throw new IllegalStateException(
                    "CONTRADICTION DETECTED: Both 'mysql' and 'standalone' profiles are simultaneously active! " +
                    "Deactivate 'standalone' by setting SPRING_PROFILES_ACTIVE=mysql to use authoritative MySQL."
            );
        }

        if (isMysqlActive) {
            String dbUrl = environment.getProperty("spring.datasource.url");
            String dbUser = environment.getProperty("spring.datasource.username");
            String dbPassword = environment.getProperty("spring.datasource.password");

            if (dbUrl == null || dbUrl.isBlank()) {
                throw new IllegalStateException(
                        "CONFIGURATION ERROR: Active profile is 'mysql', but DB_URL environment variable is missing or empty! " +
                        "The application will not start without a valid MySQL connection URL to prevent silent fallback to H2."
                );
            }

            if (dbUser == null || dbUser.isBlank()) {
                throw new IllegalStateException(
                        "CONFIGURATION ERROR: Active profile is 'mysql', but DB_USER environment variable is missing or empty! " +
                        "The application will not start without valid MySQL credentials."
                );
            }

            if (dbPassword == null || dbPassword.isBlank()) {
                throw new IllegalStateException(
                        "CONFIGURATION ERROR: Active profile is 'mysql', but DB_PASSWORD environment variable is missing or empty! " +
                        "The application will not start without valid MySQL credentials."
                );
            }

            if (dbUrl.toLowerCase().contains("jdbc:h2:")) {
                throw new IllegalStateException(
                        "CRITICAL INVARIANT VIOLATION: Active profile is 'mysql', but datasource URL points to H2 (" + dbUrl + ")! " +
                        "Silent switching to H2 under 'mysql' profile is strictly prohibited."
                );
            }

            // Extract host and database name cleanly without credentials or query params
            String sanitizedTarget = extractSanitizedEndpoint(dbUrl);
            log.info("Authoritative MySQL database profile activated. Connecting to endpoint: [{}] with user: [{}].",
                    sanitizedTarget, dbUser);
        } else if (isStandaloneActive || activeProfiles.isEmpty()) {
            log.info("Standalone in-memory H2 profile activated for zero-dependency development.");
        }
    }

    private String extractSanitizedEndpoint(String url) {
        try {
            int qIndex = url.indexOf('?');
            return (qIndex != -1) ? url.substring(0, qIndex) : url;
        } catch (Exception e) {
            return "jdbc:mysql://[REDACTED_ENDPOINT]";
        }
    }
}
