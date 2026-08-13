package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientRepositoryTest {

    private Connection transactionConnection;
    private PatientRepository repository;

    @BeforeEach
    void beginTransaction() throws Exception {
        transactionConnection = new DatabaseConfig().getConnection();
        transactionConnection.setAutoCommit(false);
        Connection nonClosingConnection = nonClosing(transactionConnection);
        repository = new PatientRepository(() -> nonClosingConnection);
    }

    @AfterEach
    void rollBackTransaction() throws Exception {
        if (transactionConnection != null) {
            try {
                transactionConnection.rollback();
            } finally {
                transactionConnection.close();
            }
        }
    }

    @Test
    void saveCreatesPatient() {
        Patient patient = completePatient(uniqueName());

        Patient saved = repository.save(patient);
        Patient loaded = repository.findById(saved.getId());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertPatientFieldsEqual(patient, loaded);
    }

    @Test
    void findByIdReturnsPatient() {
        Patient saved = repository.save(completePatient(uniqueName()));

        Patient found = repository.findById(saved.getId());

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertPatientFieldsEqual(saved, found);
        assertEquals(saved.getCreatedAt(), found.getCreatedAt());
        assertEquals(saved.getUpdatedAt(), found.getUpdatedAt());
    }

    @Test
    void findByIdReturnsNullForMissingId() {
        assertNull(repository.findById(Long.MAX_VALUE));
    }

    @Test
    void findAllReturnsPatients() {
        Patient saved = repository.save(completePatient(uniqueName()));

        List<Patient> patients = repository.findAll();

        assertTrue(patients.stream().anyMatch(patient -> saved.getId().equals(patient.getId())));
    }

    @Test
    void findByNameSupportsPartialCaseInsensitiveSearch() {
        String uniqueName = "PhaseFour" + UUID.randomUUID().toString().replace("-", "") + "Nguyen";
        Patient saved = repository.save(completePatient(uniqueName));
        String partialDifferentCase = uniqueName.substring(4, uniqueName.length() - 3).toLowerCase();

        List<Patient> patients = repository.findByName(partialDifferentCase);

        assertTrue(patients.stream().anyMatch(patient -> saved.getId().equals(patient.getId())));
    }

    @Test
    void updateChangesPatient() throws Exception {
        Patient saved = repository.save(completePatient(uniqueName()));
        makeUpdatedTimestampOlder(saved.getId());
        OffsetDateTime previousUpdatedAt = repository.findById(saved.getId()).getUpdatedAt();

        saved.setHoVaTen(uniqueName());
        saved.setGioiTinh("Nu");
        saved.setNgaySinh(LocalDate.of(1991, 2, 3));
        saved.setGiayToTuyThan("UPDATED-ID");
        saved.setSoTheBhyt("UPDATED-BHYT");
        saved.setDiaChi("Hue");
        saved.setNgheNghiep("Bac si");
        saved.setDanToc("Tay");
        repository.update(saved);

        Patient updated = repository.findById(saved.getId());
        assertPatientFieldsEqual(saved, updated);
        assertTrue(updated.getUpdatedAt().isAfter(previousUpdatedAt));
        assertEquals(saved.getCreatedAt(), updated.getCreatedAt());
    }

    @Test
    void deleteRemovesPatient() {
        Patient saved = repository.save(completePatient(uniqueName()));

        repository.delete(saved.getId());

        assertNull(repository.findById(saved.getId()));
    }

    @Test
    void optionalFieldsCanBeNull() {
        Patient patient = new Patient(uniqueName(), "Nam", LocalDate.of(1988, 5, 6));

        Patient saved = repository.save(patient);
        Patient loaded = repository.findById(saved.getId());

        assertNull(loaded.getGiayToTuyThan());
        assertNull(loaded.getSoTheBhyt());
        assertNull(loaded.getDiaChi());
        assertNull(loaded.getNgheNghiep());
        assertNull(loaded.getDanToc());
    }

    @Test
    void sqlInjectionIsNotPossibleThroughNameSearch() {
        String uniqueName = uniqueName();
        Patient saved = repository.save(completePatient(uniqueName));

        List<Patient> patients = repository.findByName(uniqueName + "%' OR '1'='1");

        assertFalse(patients.stream().anyMatch(patient -> saved.getId().equals(patient.getId())));
        assertTrue(patients.isEmpty());
    }

    private void makeUpdatedTimestampOlder(Long patientId) throws Exception {
        try (PreparedStatement statement = transactionConnection.prepareStatement("""
                UPDATE patients
                SET updated_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
                WHERE id = ?
                """)) {
            statement.setLong(1, patientId);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static Patient completePatient(String name) {
        Patient patient = new Patient(name, "Nam", LocalDate.of(1990, 1, 2));
        patient.setGiayToTuyThan("CCCD-" + UUID.randomUUID());
        patient.setSoTheBhyt("BHYT-" + UUID.randomUUID());
        patient.setDiaChi("Da Nang");
        patient.setNgheNghiep("Ky su");
        patient.setDanToc("Kinh");
        return patient;
    }

    private static String uniqueName() {
        return "Phase4 Test " + UUID.randomUUID();
    }

    private static void assertPatientFieldsEqual(Patient expected, Patient actual) {
        assertNotNull(actual);
        assertEquals(expected.getHoVaTen(), actual.getHoVaTen());
        assertEquals(expected.getGioiTinh(), actual.getGioiTinh());
        assertEquals(expected.getNgaySinh(), actual.getNgaySinh());
        assertEquals(expected.getGiayToTuyThan(), actual.getGiayToTuyThan());
        assertEquals(expected.getSoTheBhyt(), actual.getSoTheBhyt());
        assertEquals(expected.getDiaChi(), actual.getDiaChi());
        assertEquals(expected.getNgheNghiep(), actual.getNgheNghiep());
        assertEquals(expected.getDanToc(), actual.getDanToc());
    }

    private static Connection nonClosing(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) {
                        return null;
                    }
                    try {
                        return method.invoke(delegate, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
        );
    }
}
