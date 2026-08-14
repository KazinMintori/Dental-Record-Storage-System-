package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Visit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VisitRepository {

    private static final String VISIT_COLUMNS = """
            id, patient_id, tt, ngay_kham, trieu_chung, chan_doan,
            phuong_phap_dieu_tri, bac_si_kham, ghi_chu, created_at, updated_at
            """;

    private static final String INSERT_SQL = """
            INSERT INTO visits (
                patient_id, tt, ngay_kham, trieu_chung, chan_doan,
                phuong_phap_dieu_tri, bac_si_kham, ghi_chu
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT %s FROM visits WHERE id = ?
            """.formatted(VISIT_COLUMNS);

    private static final String FIND_BY_PATIENT_ID_SQL = """
            SELECT %s
            FROM visits
            WHERE patient_id = ?
            ORDER BY ngay_kham ASC, id ASC
            """.formatted(VISIT_COLUMNS);

    private static final String FIND_BY_DATE_RANGE_SQL = """
            SELECT %s
            FROM visits
            WHERE ngay_kham >= ? AND ngay_kham <= ?
            ORDER BY ngay_kham ASC, id ASC
            """.formatted(VISIT_COLUMNS);

    private static final String UPDATE_SQL = """
            UPDATE visits
            SET patient_id = ?,
                tt = ?,
                ngay_kham = ?,
                trieu_chung = ?,
                chan_doan = ?,
                phuong_phap_dieu_tri = ?,
                bac_si_kham = ?,
                ghi_chu = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND patient_id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM visits WHERE id = ?";

    private final ConnectionProvider connectionProvider;

    public VisitRepository() {
        this(new DatabaseConfig());
    }

    public VisitRepository(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig must not be null");
        this.connectionProvider = databaseConfig::getConnection;
    }

    protected VisitRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    public Visit save(Visit visit) {
        Objects.requireNonNull(visit, "visit must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindVisitFields(statement, visit);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new RepositoryException("Visit was inserted but generated values were not returned.");
                }
                return new Visit(
                        results.getObject("id", Long.class),
                        visit.getPatientId(),
                        visit.getTt(),
                        visit.getNgayKham(),
                        visit.getTrieuChung(),
                        visit.getChanDoan(),
                        visit.getPhuongPhapDieuTri(),
                        visit.getBacSiKham(),
                        visit.getGhiChu(),
                        results.getObject("created_at", OffsetDateTime.class),
                        results.getObject("updated_at", OffsetDateTime.class)
                );
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save the visit.", exception);
        }
    }

    public Visit findById(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapVisit(results) : null;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find the visit by ID.", exception);
        }
    }

    public List<Visit> findByPatientId(Long patientId) {
        Objects.requireNonNull(patientId, "patientId must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_PATIENT_ID_SQL)) {
            statement.setLong(1, patientId);
            try (ResultSet results = statement.executeQuery()) {
                return mapVisits(results);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load visits for the patient.", exception);
        }
    }

    public List<Visit> findByDateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must not be after the end date.");
        }

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_DATE_RANGE_SQL)) {
            statement.setObject(1, from);
            statement.setObject(2, to);
            try (ResultSet results = statement.executeQuery()) {
                return mapVisits(results);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load visits for the date range.", exception);
        }
    }

    /**
     * Updates an existing visit.
     *
     * @throws RepositoryException when the visit has no ID or the ID does not identify an existing row
     */
    public void update(Visit visit) {
        Objects.requireNonNull(visit, "visit must not be null");
        if (visit.getId() == null) {
            throw new RepositoryException("Cannot update a visit without an ID.");
        }

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindVisitFields(statement, visit);
            statement.setLong(9, visit.getId());
            statement.setLong(10, visit.getPatientId());
            if (statement.executeUpdate() == 0) {
                throw new RepositoryException("Cannot update a visit outside its patient record.");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update the visit.", exception);
        }
    }

    /**
     * Deletes the visit with the supplied ID. If no row has that ID, this method has no effect.
     */
    public void delete(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not delete the visit.", exception);
        }
    }

    private static void bindVisitFields(PreparedStatement statement, Visit visit) throws SQLException {
        statement.setLong(1, visit.getPatientId());
        statement.setInt(2, visit.getTt());
        statement.setObject(3, visit.getNgayKham());
        statement.setString(4, visit.getTrieuChung());
        statement.setString(5, visit.getChanDoan());
        statement.setString(6, visit.getPhuongPhapDieuTri());
        statement.setString(7, visit.getBacSiKham());
        statement.setString(8, visit.getGhiChu());
    }

    private static List<Visit> mapVisits(ResultSet results) throws SQLException {
        List<Visit> visits = new ArrayList<>();
        while (results.next()) {
            visits.add(mapVisit(results));
        }
        return visits;
    }

    private static Visit mapVisit(ResultSet results) throws SQLException {
        return new Visit(
                results.getObject("id", Long.class),
                results.getObject("patient_id", Long.class),
                results.getObject("tt", Integer.class),
                results.getObject("ngay_kham", LocalDate.class),
                results.getString("trieu_chung"),
                results.getString("chan_doan"),
                results.getString("phuong_phap_dieu_tri"),
                results.getString("bac_si_kham"),
                results.getString("ghi_chu"),
                results.getObject("created_at", OffsetDateTime.class),
                results.getObject("updated_at", OffsetDateTime.class)
        );
    }

    @FunctionalInterface
    protected interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
