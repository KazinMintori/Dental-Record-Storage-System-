package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;
import com.dentalclinic.model.PatientPage;
import com.dentalclinic.model.PatientSearchCriteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class PatientRepository {

    private static final String PATIENT_COLUMNS = """
            id, ho_va_ten, gioi_tinh, ngay_sinh, so_dien_thoai, giay_to_tuy_than,
            so_the_bhyt, dia_chi, nghe_nghiep, dan_toc, created_at, updated_at, deleted_at
            """;

    private static final String INSERT_SQL = """
            INSERT INTO patients (
                ho_va_ten, gioi_tinh, ngay_sinh, so_dien_thoai, giay_to_tuy_than,
                so_the_bhyt, dia_chi, nghe_nghiep, dan_toc
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT %s
            FROM patients
            WHERE id = ? AND deleted_at IS NULL
            """.formatted(PATIENT_COLUMNS);

    private static final String FIND_ALL_SQL = """
            SELECT %s
            FROM patients
            WHERE deleted_at IS NULL
            ORDER BY ho_va_ten ASC
            """.formatted(PATIENT_COLUMNS);

    private static final String FIND_DELETED_SQL = """
            SELECT %s
            FROM patients
            WHERE deleted_at IS NOT NULL
            ORDER BY deleted_at DESC, ho_va_ten ASC
            """.formatted(PATIENT_COLUMNS);

    private static final String UPDATE_SQL = """
            UPDATE patients
            SET ho_va_ten = ?,
                gioi_tinh = ?,
                ngay_sinh = ?,
                so_dien_thoai = ?,
                giay_to_tuy_than = ?,
                so_the_bhyt = ?,
                dia_chi = ?,
                nghe_nghiep = ?,
                dan_toc = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted_at IS NULL
            """;

    private static final String MOVE_TO_TRASH_SQL = """
            UPDATE patients
            SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted_at IS NULL
            """;
    private static final String RESTORE_SQL = """
            UPDATE patients
            SET deleted_at = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted_at IS NOT NULL
            """;
    private static final String PERMANENTLY_DELETE_SQL =
            "DELETE FROM patients WHERE id = ? AND deleted_at IS NOT NULL";

    private final ConnectionProvider connectionProvider;

    public PatientRepository() {
        this(new DatabaseConfig());
    }

    public PatientRepository(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig must not be null");
        this.connectionProvider = databaseConfig::getConnection;
    }

    protected PatientRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    public Patient save(Patient patient) {
        Objects.requireNonNull(patient, "patient must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindPatientFields(statement, patient);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new RepositoryException("Patient was inserted but generated values were not returned.");
                }
                return new Patient(
                        results.getObject("id", Long.class),
                        patient.getHoVaTen(),
                        patient.getGioiTinh(),
                        patient.getNgaySinh(),
                        patient.getSoDienThoai(),
                        patient.getGiayToTuyThan(),
                        patient.getSoTheBhyt(),
                        patient.getDiaChi(),
                        patient.getNgheNghiep(),
                        patient.getDanToc(),
                        results.getObject("created_at", java.time.OffsetDateTime.class),
                        results.getObject("updated_at", java.time.OffsetDateTime.class),
                        null
                );
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save the patient.", exception);
        }
    }

    public Patient findById(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);

            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapPatient(results) : null;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find the patient by ID.", exception);
        }
    }

    public List<Patient> findAll() {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet results = statement.executeQuery()) {
            return mapPatients(results);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load patients.", exception);
        }
    }

    public List<Patient> findDeleted() {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_DELETED_SQL);
             ResultSet results = statement.executeQuery()) {
            return mapPatients(results);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load deleted patients.", exception);
        }
    }

    public List<Patient> findByName(String name) {
        return search(new PatientSearchCriteria(name, null, null, null, null));
    }

    public PatientPage searchPage(PatientSearchCriteria criteria, int pageIndex, int pageSize) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        validatePageRequest(pageIndex, pageSize);
        QueryParts query = activeSearchQuery(criteria);

        try (Connection connection = connectionProvider.getConnection()) {
            long total = count(connection, "SELECT COUNT(*) FROM patients\n" + query.whereClause(),
                    query.parameters());
            int effectivePage = effectivePage(pageIndex, pageSize, total);
            List<Object> parameters = new ArrayList<>(query.parameters());
            StringBuilder sql = new StringBuilder("SELECT ")
                    .append(PATIENT_COLUMNS)
                    .append("\nFROM patients\n")
                    .append(query.whereClause());
            appendSearchOrdering(sql, criteria, parameters);
            sql.append("\nLIMIT ? OFFSET ?");
            parameters.add(pageSize);
            parameters.add((long) effectivePage * pageSize);

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                bindParameters(statement, parameters);
                try (ResultSet results = statement.executeQuery()) {
                    return new PatientPage(mapPatients(results), total, effectivePage, pageSize);
                }
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load the patient page.", exception);
        }
    }

    public PatientPage findDeletedPage(int pageIndex, int pageSize) {
        validatePageRequest(pageIndex, pageSize);
        try (Connection connection = connectionProvider.getConnection()) {
            long total = count(connection,
                    "SELECT COUNT(*) FROM patients WHERE deleted_at IS NOT NULL", List.of());
            int effectivePage = effectivePage(pageIndex, pageSize, total);
            String sql = "SELECT " + PATIENT_COLUMNS + " FROM patients "
                    + "WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC, ho_va_ten ASC "
                    + "LIMIT ? OFFSET ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, pageSize);
                statement.setLong(2, (long) effectivePage * pageSize);
                try (ResultSet results = statement.executeQuery()) {
                    return new PatientPage(mapPatients(results), total, effectivePage, pageSize);
                }
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load the deleted-patient page.", exception);
        }
    }

    /**
     * Searches in PostgreSQL. Name tokens and every advanced filter are combined with AND;
     * no patient rows are filtered in Java.
     */
    public List<Patient> search(PatientSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        QueryParts query = activeSearchQuery(criteria);
        List<Object> parameters = new ArrayList<>(query.parameters());

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(PATIENT_COLUMNS)
                .append("\nFROM patients\n")
                .append(query.whereClause());
        appendSearchOrdering(sql, criteria, parameters);

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);

            try (ResultSet results = statement.executeQuery()) {
                return mapPatients(results);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not search for patients.", exception);
        }
    }

    /**
     * Updates an existing patient.
     *
     * @throws RepositoryException when the patient has no ID or the ID does not identify an existing row
     */
    public void update(Patient patient) {
        Objects.requireNonNull(patient, "patient must not be null");
        if (patient.getId() == null) {
            throw new RepositoryException("Cannot update a patient without an ID.");
        }

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindPatientFields(statement, patient);
            statement.setLong(10, patient.getId());

            if (statement.executeUpdate() == 0) {
                throw new RepositoryException("Cannot update a patient that does not exist.");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update the patient.", exception);
        }
    }

    /** Moves a patient to the trash without deleting visits or revenue rows. */
    public void delete(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(MOVE_TO_TRASH_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not move the patient to trash.", exception);
        }
    }

    public void deleteAll(List<Long> ids) {
        executeBulkMutation(ids,
                "UPDATE patients SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE deleted_at IS NULL AND id IN (%s)",
                "Could not move patients to trash.");
    }

    public void restoreAll(List<Long> ids) {
        executeBulkMutation(ids,
                "UPDATE patients SET deleted_at = NULL, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE deleted_at IS NOT NULL AND id IN (%s)",
                "Could not restore patients.");
    }

    /** Permanently deletes only patients which are already in the trash. */
    public void permanentlyDeleteAll(List<Long> ids) {
        executeBulkMutation(ids,
                "DELETE FROM patients WHERE deleted_at IS NOT NULL AND id IN (%s)",
                "Could not permanently delete patients.");
    }

    private void executeBulkMutation(List<Long> ids, String sqlTemplate, String errorMessage) {
        Objects.requireNonNull(ids, "ids must not be null");
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(distinctIds.size(), "?"));
        String sql = sqlTemplate.formatted(placeholders);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < distinctIds.size(); index++) {
                statement.setLong(index + 1, distinctIds.get(index));
            }
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException(errorMessage, exception);
        }
    }

    public void restore(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(RESTORE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not restore the patient.", exception);
        }
    }

    /** Permanently deletes a trashed patient; database cascades remove visits and revenue rows. */
    public void permanentlyDelete(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(PERMANENTLY_DELETE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not permanently delete the patient.", exception);
        }
    }

    private static void bindPatientFields(PreparedStatement statement, Patient patient) throws SQLException {
        statement.setString(1, patient.getHoVaTen());
        statement.setString(2, patient.getGioiTinh());
        statement.setObject(3, patient.getNgaySinh());
        statement.setString(4, patient.getSoDienThoai());
        statement.setString(5, patient.getGiayToTuyThan());
        statement.setString(6, patient.getSoTheBhyt());
        statement.setString(7, patient.getDiaChi());
        statement.setString(8, patient.getNgheNghiep());
        statement.setString(9, patient.getDanToc());
    }

    private static QueryParts activeSearchQuery(PatientSearchCriteria criteria) {
        List<Object> parameters = new ArrayList<>();
        StringJoiner conditions = new StringJoiner("\nAND ", "WHERE ", "\n");
        conditions.add("deleted_at IS NULL");
        for (String token : criteria.nameTokens()) {
            conditions.add("ho_va_ten_search LIKE '%' || public.normalize_vietnamese(?) || '%'");
            parameters.add(token);
        }
        if (criteria.patientCode() != null) {
            Long patientId = parsePatientId(criteria.patientCode());
            if (patientId == null) {
                conditions.add("FALSE");
            } else {
                conditions.add("id = ?");
                parameters.add(patientId);
            }
        }
        if (criteria.phone() != null) {
            conditions.add("replace(coalesce(so_dien_thoai, ''), ' ', '') ILIKE '%' || replace(?, ' ', '') || '%'");
            parameters.add(criteria.phone());
        }
        if (criteria.birthDate() != null) {
            conditions.add("ngay_sinh = ?");
            parameters.add(criteria.birthDate());
        }
        if (criteria.gender() != null) {
            conditions.add("public.normalize_vietnamese(gioi_tinh) = public.normalize_vietnamese(?)");
            parameters.add(criteria.gender());
        }
        return new QueryParts(conditions.toString(), List.copyOf(parameters));
    }

    private static Long parsePatientId(String patientCode) {
        String normalized = patientCode.strip();
        if (normalized.regionMatches(true, 0, "BN-", 0, 3)) {
            normalized = normalized.substring(3);
        }
        if (normalized.isEmpty() || !normalized.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void appendSearchOrdering(
            StringBuilder sql, PatientSearchCriteria criteria, List<Object> parameters) {
        if (criteria.name() != null) {
            sql.append("""
                    ORDER BY CASE
                        WHEN ho_va_ten_search = public.normalize_vietnamese(?) THEN 0
                        WHEN ho_va_ten_search LIKE '%' || public.normalize_vietnamese(?) || '%' THEN 1
                        ELSE 2
                    END,
                    ho_va_ten_search ASC,
                    id ASC
                    """);
            parameters.add(criteria.name());
            parameters.add(criteria.name());
        } else {
            sql.append("ORDER BY ho_va_ten_search ASC, id ASC");
        }
    }

    private static long count(Connection connection, String sql, List<Object> parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("Count query did not return a row.");
                }
                return results.getLong(1);
            }
        }
    }

    private static void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private static void validatePageRequest(int pageIndex, int pageSize) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must not be negative");
        }
        if (pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("pageSize must be between 1 and 500");
        }
    }

    private static int effectivePage(int requestedPage, int pageSize, long total) {
        int lastPage = total == 0 ? 0 : (int) ((total - 1) / pageSize);
        return Math.min(requestedPage, lastPage);
    }

    private record QueryParts(String whereClause, List<Object> parameters) {
    }

    private static List<Patient> mapPatients(ResultSet results) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        while (results.next()) {
            patients.add(mapPatient(results));
        }
        return patients;
    }

    private static Patient mapPatient(ResultSet results) throws SQLException {
        return new Patient(
                results.getObject("id", Long.class),
                results.getString("ho_va_ten"),
                results.getString("gioi_tinh"),
                results.getObject("ngay_sinh", java.time.LocalDate.class),
                results.getString("so_dien_thoai"),
                results.getString("giay_to_tuy_than"),
                results.getString("so_the_bhyt"),
                results.getString("dia_chi"),
                results.getString("nghe_nghiep"),
                results.getString("dan_toc"),
                results.getObject("created_at", java.time.OffsetDateTime.class),
                results.getObject("updated_at", java.time.OffsetDateTime.class),
                results.getObject("deleted_at", java.time.OffsetDateTime.class)
        );
    }

    @FunctionalInterface
    protected interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
