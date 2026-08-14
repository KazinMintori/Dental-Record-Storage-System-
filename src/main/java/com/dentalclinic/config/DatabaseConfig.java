package com.dentalclinic.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class DatabaseConfig {

    private static final Object POOL_LOCK = new Object();
    private static HikariDataSource sharedDataSource;

    public static final String HOST_ENVIRONMENT_VARIABLE = "SUPABASE_DB_HOST";
    public static final String PORT_ENVIRONMENT_VARIABLE = "SUPABASE_DB_PORT";
    public static final String DATABASE_ENVIRONMENT_VARIABLE = "SUPABASE_DB_NAME";
    public static final String USER_ENVIRONMENT_VARIABLE = "SUPABASE_DB_USER";
    public static final String PASSWORD_ENVIRONMENT_VARIABLE = "SUPABASE_DB_PASSWORD";

    private static final List<String> REQUIRED_ENVIRONMENT_VARIABLES = List.of(
            HOST_ENVIRONMENT_VARIABLE,
            PORT_ENVIRONMENT_VARIABLE,
            DATABASE_ENVIRONMENT_VARIABLE,
            USER_ENVIRONMENT_VARIABLE,
            PASSWORD_ENVIRONMENT_VARIABLE
    );

    private final String url;
    private final String username;
    private final String password;

    public DatabaseConfig() {
        this(System.getenv());
    }

    DatabaseConfig(Map<String, String> environment) {
        List<String> missingVariables = findMissingEnvironmentVariables(environment);
        if (!missingVariables.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required Supabase database environment variables: "
                            + String.join(", ", missingVariables)
            );
        }

        String host = environment.get(HOST_ENVIRONMENT_VARIABLE).trim();
        String port = validatePort(environment.get(PORT_ENVIRONMENT_VARIABLE));
        String database = environment.get(DATABASE_ENVIRONMENT_VARIABLE).trim();
        this.username = environment.get(USER_ENVIRONMENT_VARIABLE).trim();
        this.password = environment.get(PASSWORD_ENVIRONMENT_VARIABLE);
        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + database
                + "?sslmode=require&tcpKeepAlive=true";
    }

    public Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public static void shutdownConnectionPool() {
        synchronized (POOL_LOCK) {
            if (sharedDataSource != null) {
                sharedDataSource.close();
                sharedDataSource = null;
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    private HikariDataSource dataSource() {
        synchronized (POOL_LOCK) {
            if (sharedDataSource == null || sharedDataSource.isClosed()) {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(url);
                config.setUsername(username);
                config.setPassword(password);
                config.setPoolName("dental-supabase-pool");
                config.setMinimumIdle(1);
                config.setMaximumPoolSize(4);
                config.setConnectionTimeout(10_000);
                config.setValidationTimeout(5_000);
                config.setIdleTimeout(300_000);
                config.setMaxLifetime(900_000);
                config.setKeepaliveTime(120_000);
                sharedDataSource = new HikariDataSource(config);
            }
            return sharedDataSource;
        }
    }

    public static List<String> findMissingEnvironmentVariables() {
        return findMissingEnvironmentVariables(System.getenv());
    }

    private static List<String> findMissingEnvironmentVariables(Map<String, String> environment) {
        return REQUIRED_ENVIRONMENT_VARIABLES.stream()
                .filter(name -> environment.get(name) == null || environment.get(name).isBlank())
                .toList();
    }

    private static String validatePort(String value) {
        String port = value.trim();
        try {
            int portNumber = Integer.parseInt(port);
            if (portNumber < 1 || portNumber > 65_535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(PORT_ENVIRONMENT_VARIABLE + " must be a number from 1 to 65535.");
        }
        return port;
    }
}
