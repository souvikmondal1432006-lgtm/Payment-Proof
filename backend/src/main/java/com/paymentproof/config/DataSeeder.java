package com.paymentproof.config;

import com.paymentproof.repository.IncidentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final IncidentCaseRepository incidentCaseRepository;
    private final DataSource dataSource;

    @org.springframework.beans.factory.annotation.Value("${app.database.auto-seed:true}")
    private boolean autoSeedEnabled;

    @Override
    public void run(String... args) {
        if (!autoSeedEnabled) {
            log.info("Automatic database seeding is disabled (app.database.auto-seed=false). Skipping.");
            return;
        }

        long count = incidentCaseRepository.count();
        if (count == 0) {
            log.info("Incident repository is empty (count = 0). Automatically seeding forensic payment records from seed.sql...");
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("seed.sql"));
                populator.setContinueOnError(true);
                populator.setIgnoreFailedDrops(true);
                populator.execute(dataSource);
                long newCount = incidentCaseRepository.count();
                log.info("Database seeding complete! Loaded {} forensic incident cases into store.", newCount);
            } catch (Exception e) {
                log.error("Failed to seed database from seed.sql: {}", e.getMessage(), e);
            }
        } else {
            log.info("Database already contains {} incident cases. Skipping seed data insertion.", count);
        }
    }
}
