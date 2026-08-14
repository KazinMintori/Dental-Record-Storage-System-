package com.dentalclinic;

import com.dentalclinic.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DatabaseConnectionTest {

    @Test
    void connectsToSupabasePostgreSql() {
        List<String> missingVariables = DatabaseConfig.findMissingEnvironmentVariables();
        assertTrue(
                missingVariables.isEmpty(),
                () -> "Supabase database test cannot run. Set these environment variables: "
                        + String.join(", ", missingVariables)
        );

        DatabaseConfig config = new DatabaseConfig();
        try (Connection connection = config.getConnection()) {
            assertTrue(
                    connection.isValid(5),
                    "Supabase PostgreSQL accepted the connection, but it was not valid within 5 seconds."
            );
        } catch (IllegalStateException exception) {
            fail(exception.getMessage(), exception);
        } catch (SQLException exception) {
            fail(
                    "Could not connect to Supabase PostgreSQL at " + config.getUrl()
                            + " as user " + config.getUsername()
                            + ". Confirm the Supabase host, port, database, user, password, network access, "
                            + "and project status. SQL state: " + exception.getSQLState()
                            + ". The password has not been included in this message."
            );
        }
    }

    @Test
    void databaseAndJdbcConnectionUseUtf8() throws SQLException {
        assertTrue(DatabaseConfig.findMissingEnvironmentVariables().isEmpty(),
                "Supabase environment variables are required for the encoding integration test.");
        try (Connection connection = new DatabaseConfig().getConnection()) {
            assertEquals("UTF8", setting(connection, "server_encoding"));
            assertEquals("UTF8", setting(connection, "client_encoding"));
        }
    }

    private static String setting(Connection connection, String name) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT current_setting(?)")) {
            statement.setString(1, name);
            try (var results = statement.executeQuery()) {
                assertTrue(results.next());
                return results.getString(1);
            }
        }
    }
}
