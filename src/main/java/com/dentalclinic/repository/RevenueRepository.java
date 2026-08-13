package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Revenue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RevenueRepository {

    private static final String REVENUE_COLUMNS =
            "id, visit_id, so_hieu, ngay_thang, dien_giai, so_tien, created_at";

    private static final String INSERT_SQL = """
            INSERT INTO doanh_thu (visit_id, so_hieu, ngay_thang, dien_giai, so_tien)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, created_at
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT %s FROM doanh_thu WHERE id = ?
            """.formatted(REVENUE_COLUMNS);

    private static final String FIND_BY_VISIT_ID_SQL = """
            SELECT %s
            FROM doanh_thu
            WHERE visit_id = ?
            ORDER BY ngay_thang ASC, id ASC
            """.formatted(REVENUE_COLUMNS);

    private static final String FIND_BY_DATE_RANGE_SQL = """
            SELECT %s
            FROM doanh_thu
            WHERE ngay_thang >= ? AND ngay_thang <= ?
            ORDER BY ngay_thang ASC, id ASC
            """.formatted(REVENUE_COLUMNS);

    private static final String CALCULATE_TOTAL_SQL = """
            SELECT COALESCE(SUM(so_tien), 0)
            FROM doanh_thu
            WHERE ngay_thang >= ? AND ngay_thang <= ?
            """;

    private static final String UPDATE_SQL = """
            UPDATE doanh_thu
            SET visit_id = ?, so_hieu = ?, ngay_thang = ?, dien_giai = ?, so_tien = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM doanh_thu WHERE id = ?";

    private final ConnectionProvider connectionProvider;

    public RevenueRepository() {
        this(new DatabaseConfig());
    }

    public RevenueRepository(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig must not be null");
        this.connectionProvider = databaseConfig::getConnection;
    }

    RevenueRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    public Revenue save(Revenue revenue) {
        Objects.requireNonNull(revenue, "revenue must not be null");
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindRevenueFields(statement, revenue);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new RepositoryException("Revenue was inserted but generated values were not returned.");
                }
                return new Revenue(
                        results.getObject("id", Long.class), revenue.getVisitId(), revenue.getSoHieu(),
                        revenue.getNgayThang(), revenue.getDienGiai(), revenue.getSoTien(),
                        results.getObject("created_at", OffsetDateTime.class)
                );
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save the revenue entry.", exception);
        }
    }

    public Revenue findById(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapRevenue(results) : null;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find the revenue entry by ID.", exception);
        }
    }

    public List<Revenue> findByVisitId(Long visitId) {
        Objects.requireNonNull(visitId, "visitId must not be null");
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_VISIT_ID_SQL)) {
            statement.setLong(1, visitId);
            try (ResultSet results = statement.executeQuery()) {
                return mapRevenues(results);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load revenue entries for the visit.", exception);
        }
    }

    public List<Revenue> findByDateRange(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_DATE_RANGE_SQL)) {
            bindDateRange(statement, from, to);
            try (ResultSet results = statement.executeQuery()) {
                return mapRevenues(results);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load revenue entries for the date range.", exception);
        }
    }

    public BigDecimal calculateTotalByDateRange(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(CALCULATE_TOTAL_SQL)) {
            bindDateRange(statement, from, to);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new RepositoryException("The revenue total query returned no result.");
                }
                BigDecimal total = results.getBigDecimal(1);
                return total == null ? BigDecimal.ZERO : total;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not calculate the revenue total for the date range.", exception);
        }
    }

    /**
     * @throws RepositoryException when the entry has no ID or does not exist
     */
    public void update(Revenue revenue) {
        Objects.requireNonNull(revenue, "revenue must not be null");
        if (revenue.getId() == null) {
            throw new RepositoryException("Cannot update a revenue entry without an ID.");
        }
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindRevenueFields(statement, revenue);
            statement.setLong(6, revenue.getId());
            if (statement.executeUpdate() == 0) {
                throw new RepositoryException("Cannot update a revenue entry that does not exist.");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update the revenue entry.", exception);
        }
    }

    /** Deletes the supplied entry if it exists. */
    public void delete(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not delete the revenue entry.", exception);
        }
    }

    private static void bindRevenueFields(PreparedStatement statement, Revenue revenue) throws SQLException {
        statement.setLong(1, revenue.getVisitId());
        statement.setString(2, revenue.getSoHieu());
        statement.setObject(3, revenue.getNgayThang());
        statement.setString(4, revenue.getDienGiai());
        statement.setBigDecimal(5, revenue.getSoTien());
    }

    private static void bindDateRange(PreparedStatement statement, LocalDate from, LocalDate to) throws SQLException {
        statement.setObject(1, from);
        statement.setObject(2, to);
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must not be after the end date.");
        }
    }

    private static List<Revenue> mapRevenues(ResultSet results) throws SQLException {
        List<Revenue> revenues = new ArrayList<>();
        while (results.next()) {
            revenues.add(mapRevenue(results));
        }
        return revenues;
    }

    private static Revenue mapRevenue(ResultSet results) throws SQLException {
        return new Revenue(
                results.getObject("id", Long.class),
                results.getObject("visit_id", Long.class),
                results.getString("so_hieu"),
                results.getObject("ngay_thang", LocalDate.class),
                results.getString("dien_giai"),
                results.getBigDecimal("so_tien"),
                results.getObject("created_at", OffsetDateTime.class)
        );
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
