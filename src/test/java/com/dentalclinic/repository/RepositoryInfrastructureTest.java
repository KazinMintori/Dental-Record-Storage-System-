package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryInfrastructureTest {

    @Test
    void repositoryExceptionSupportsAUsefulMessage() {
        RepositoryException exception = new RepositoryException("Could not load patient records.");

        assertEquals("Could not load patient records.", exception.getMessage());
    }

    @Test
    void repositoryExceptionPreservesTheOriginalCause() {
        SQLException cause = new SQLException("Database operation failed.");
        RepositoryException exception = new RepositoryException("Could not load patient records.", cause);

        assertEquals("Could not load patient records.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void existingDatabaseConfigObtainsAValidConnection() throws SQLException {
        assertTrue(
                DatabaseConfig.findMissingEnvironmentVariables().isEmpty(),
                "Supabase database environment variables are required for the connection test."
        );

        try (Connection connection = new DatabaseConfig().getConnection()) {
            assertTrue(connection.isValid(5), "Supabase PostgreSQL connection must be valid.");
        }
    }
}
