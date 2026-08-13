package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatientRepository {

    private static final String PATIENT_COLUMNS = """
            id, ho_va_ten, gioi_tinh, ngay_sinh, giay_to_tuy_than,
            so_the_bhyt, dia_chi, nghe_nghiep, dan_toc, created_at, updated_at
            """;

    private static final String INSERT_SQL = """
            INSERT INTO patients (
                ho_va_ten, gioi_tinh, ngay_sinh, giay_to_tuy_than,
                so_the_bhyt, dia_chi, nghe_nghiep, dan_toc
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT %s
            FROM patients
            WHERE id = ?
            """.formatted(PATIENT_COLUMNS);

    private static final String FIND_ALL_SQL = """
            SELECT %s
            FROM patients
            ORDER BY ho_va_ten ASC
            """.formatted(PATIENT_COLUMNS);

    private static final String FIND_BY_NAME_SQL = """
            SELECT %s
            FROM patients
            WHERE ho_va_ten ILIKE ?
            ORDER BY ho_va_ten ASC
            """.formatted(PATIENT_COLUMNS);

    private static final String UPDATE_SQL = """
            UPDATE patients
            SET ho_va_ten = ?,
                gioi_tinh = ?,
                ngay_sinh = ?,
                giay_to_tuy_than = ?,
                so_the_bhyt = ?,
                dia_chi = ?,
                nghe_nghiep = ?,
                dan_toc = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM patients WHERE id = ?";

    private final ConnectionProvider connectionProvider;

    public PatientRepository() {
        this(new DatabaseConfig());
    }

    public PatientRepository(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig must not be null");
        this.connectionProvider = databaseConfig::getConnection;
    }

    PatientRepository(ConnectionProvider connectionProvider) {
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
                        patient.getGiayToTuyThan(),
                        patient.getSoTheBhyt(),
                        patient.getDiaChi(),
                        patient.getNgheNghiep(),
                        patient.getDanToc(),
                        results.getObject("created_at", java.time.OffsetDateTime.class),
                        results.getObject("updated_at", java.time.OffsetDateTime.class)
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

    public List<Patient> findByName(String name) {
        if (name == null || name.isBlank()) {
            return findAll();
        }

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_NAME_SQL)) {
            statement.setString(1, "%" + name.trim() + "%");

            try (ResultSet results = statement.executeQuery()) {
                return mapPatients(results);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not search for patients by name.", exception);
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
            statement.setLong(9, patient.getId());

            if (statement.executeUpdate() == 0) {
                throw new RepositoryException("Cannot update a patient that does not exist.");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update the patient.", exception);
        }
    }

    /**
     * Deletes the patient with the supplied ID. If no row has that ID, this method has no effect.
     */
    public void delete(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not delete the patient.", exception);
        }
    }

    private static void bindPatientFields(PreparedStatement statement, Patient patient) throws SQLException {
        statement.setString(1, patient.getHoVaTen());
        statement.setString(2, patient.getGioiTinh());
        statement.setObject(3, patient.getNgaySinh());
        statement.setString(4, patient.getGiayToTuyThan());
        statement.setString(5, patient.getSoTheBhyt());
        statement.setString(6, patient.getDiaChi());
        statement.setString(7, patient.getNgheNghiep());
        statement.setString(8, patient.getDanToc());
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
                results.getString("giay_to_tuy_than"),
                results.getString("so_the_bhyt"),
                results.getString("dia_chi"),
                results.getString("nghe_nghiep"),
                results.getString("dan_toc"),
                results.getObject("created_at", java.time.OffsetDateTime.class),
                results.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
