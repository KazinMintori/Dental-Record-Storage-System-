package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class RepositoryTransaction {

    private final ConnectionProvider connectionProvider;

    public RepositoryTransaction(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig must not be null");
        this.connectionProvider = databaseConfig::getConnection;
    }

    public RepositoryTransaction(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    public <T> T execute(Work<T> work) {
        Objects.requireNonNull(work, "work must not be null");
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            Connection sharedConnection = nonClosing(connection);
            Context context = new Context(
                    new VisitRepository(() -> sharedConnection),
                    new RevenueRepository(() -> sharedConnection)
            );
            try {
                T result = work.run(context);
                connection.commit();
                return result;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not complete the visit transaction.", exception);
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private static Connection nonClosing(Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
        );
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Context context);
    }

    public record Context(VisitRepository visitRepository, RevenueRepository revenueRepository) {
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
