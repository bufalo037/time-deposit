package org.ikigaidigital;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app against a throwaway Testcontainers PostgreSQL instead of compose.yaml:
 * {@code ./mvnw spring-boot:test-run}, or run this main method from the IDE.
 */
public class TestTimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.from(TimeDepositApplication::main)
                .with(PostgresTestcontainersConfiguration.class)
                .run(args);
    }
}
