package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;
import com.dentalclinic.model.PatientPage;
import com.dentalclinic.model.PatientSearchCriteria;
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
    void unicodeRoundTripPreservesVietnameseNameAndPhone() {
        Patient patient = new Patient("Đỗ Minh Đức", "Nam", LocalDate.of(1995, 3, 12));
        patient.setSoDienThoai("0123456789");

        Patient saved = repository.save(patient);
        Patient reloaded = repository.findById(saved.getId());

        assertEquals("Đỗ Minh Đức", reloaded.getHoVaTen());
        assertEquals("0123456789", reloaded.getSoDienThoai());
    }

    @Test
    void nameSearchIsCaseAndVietnameseAccentInsensitive() {
        Patient saved = repository.save(new Patient("Đỗ Minh Đức", "Nam", LocalDate.of(1995, 3, 12)));

        for (String query : List.of("do minh duc", "DO MINH DUC", "đỗ minh đức")) {
            assertTrue(repository.findByName(query).stream()
                    .anyMatch(patient -> saved.getId().equals(patient.getId())), query);
        }
    }

    @Test
    void nameSearchMatchesNonAdjacentWords() {
        Patient first = repository.save(new Patient("Nguyễn Văn Ánh", "Nam", LocalDate.of(1990, 1, 1)));
        Patient second = repository.save(new Patient("Nguyễn Thị Ánh", "Nữ", LocalDate.of(1991, 2, 2)));

        List<Patient> results = repository.findByName("nguyen anh");

        assertTrue(results.stream().anyMatch(patient -> first.getId().equals(patient.getId())));
        assertTrue(results.stream().anyMatch(patient -> second.getId().equals(patient.getId())));
    }

    @Test
    void combinedCriteriaUseAndSemantics() {
        Patient male = new Patient("Nguyễn Văn Nam", "Nam", LocalDate.of(1992, 4, 5));
        male.setSoDienThoai("0901234567");
        Patient female = new Patient("Nguyễn Văn Nam", "Nữ", LocalDate.of(1992, 4, 5));
        female.setSoDienThoai("0907654321");
        Patient savedMale = repository.save(male);
        Patient savedFemale = repository.save(female);

        List<Patient> results = repository.search(new PatientSearchCriteria(
                "Nguyễn Văn Nam", null, null, null, "Nam"));

        assertTrue(results.stream().anyMatch(patient -> savedMale.getId().equals(patient.getId())));
        assertFalse(results.stream().anyMatch(patient -> savedFemale.getId().equals(patient.getId())));
    }

    @Test
    void unmatchedSearchReturnsEmptyList() {
        assertTrue(repository.findByName("xyz123999-no-patient").isEmpty());
    }

    @Test
    void databasePaginationReturnsOnlyRequestedSliceAndTotalCount() {
        String prefix = uniqueName();
        repository.save(completePatient(prefix + " A"));
        repository.save(completePatient(prefix + " B"));
        repository.save(completePatient(prefix + " C"));
        PatientSearchCriteria criteria = new PatientSearchCriteria(prefix, null, null, null, null);

        PatientPage first = repository.searchPage(criteria, 0, 2);
        PatientPage second = repository.searchPage(criteria, 1, 2);

        assertEquals(3, first.totalElements());
        assertEquals(2, first.patients().size());
        assertEquals(1, second.patients().size());
        assertEquals(2, first.totalPages());
    }

    @Test
    void bulkDeleteMovesEverySelectedPatientToTrash() {
        Patient first = repository.save(completePatient(uniqueName()));
        Patient second = repository.save(completePatient(uniqueName()));

        repository.deleteAll(List.of(first.getId(), second.getId()));

        assertNull(repository.findById(first.getId()));
        assertNull(repository.findById(second.getId()));
        assertTrue(repository.findDeleted().stream()
                .map(Patient::getId).toList().containsAll(List.of(first.getId(), second.getId())));
    }

    @Test
    void bulkRestoreReturnsEverySelectedPatientToTheActiveDirectory() {
        Patient first = repository.save(completePatient(uniqueName()));
        Patient second = repository.save(completePatient(uniqueName()));
        List<Long> ids = List.of(first.getId(), second.getId());
        repository.deleteAll(ids);

        repository.restoreAll(ids);

        assertNotNull(repository.findById(first.getId()));
        assertNotNull(repository.findById(second.getId()));
        assertTrue(repository.findDeleted().stream().map(Patient::getId).noneMatch(ids::contains));
    }

    @Test
    void bulkPermanentDeleteRemovesOnlyPatientsAlreadyInTrash() {
        Patient active = repository.save(completePatient(uniqueName()));
        Patient trashed = repository.save(completePatient(uniqueName()));
        repository.deleteAll(List.of(trashed.getId()));

        repository.permanentlyDeleteAll(List.of(active.getId(), trashed.getId()));

        assertNotNull(repository.findById(active.getId()));
        assertNull(repository.findById(trashed.getId()));
    }

    @Test
    void searchRanksExactThenContiguousThenWordMatches() {
        Patient exact = repository.save(new Patient("Đỗ Minh Đức", "Nam", LocalDate.of(1990, 1, 1)));
        Patient contiguous = repository.save(new Patient("Trần Đỗ Minh Đức", "Nam", LocalDate.of(1990, 1, 1)));
        Patient partial = repository.save(new Patient("Đỗ Hoàng Minh Đức", "Nam", LocalDate.of(1990, 1, 1)));

        List<Patient> results = repository.findByName("do minh duc");

        assertTrue(indexOf(results, exact.getId()) < indexOf(results, contiguous.getId()));
        assertTrue(indexOf(results, contiguous.getId()) < indexOf(results, partial.getId()));
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
    void deleteMovesPatientToTrashAndRestoreReturnsIt() {
        Patient saved = repository.save(completePatient(uniqueName()));

        repository.delete(saved.getId());
        Patient trashed = repository.findDeleted().stream()
                .filter(patient -> saved.getId().equals(patient.getId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(trashed.getDeletedAt());
        repository.restore(saved.getId());
        assertNotNull(repository.findById(saved.getId()));
        assertFalse(repository.findDeleted().stream()
                .anyMatch(patient -> saved.getId().equals(patient.getId())));
    }

    @Test
    void permanentlyDeleteOnlyRemovesTrashedPatient() {
        Patient saved = repository.save(completePatient(uniqueName()));

        repository.permanentlyDelete(saved.getId());
        assertNotNull(repository.findById(saved.getId()));

        repository.delete(saved.getId());
        repository.permanentlyDelete(saved.getId());
        assertNull(repository.findById(saved.getId()));
        assertFalse(repository.findDeleted().stream()
                .anyMatch(patient -> saved.getId().equals(patient.getId())));
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
        patient.setSoDienThoai("0123456789");
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
        assertEquals(expected.getSoDienThoai(), actual.getSoDienThoai());
        assertEquals(expected.getGiayToTuyThan(), actual.getGiayToTuyThan());
        assertEquals(expected.getSoTheBhyt(), actual.getSoTheBhyt());
        assertEquals(expected.getDiaChi(), actual.getDiaChi());
        assertEquals(expected.getNgheNghiep(), actual.getNgheNghiep());
        assertEquals(expected.getDanToc(), actual.getDanToc());
    }

    private static int indexOf(List<Patient> patients, Long id) {
        for (int index = 0; index < patients.size(); index++) {
            if (id.equals(patients.get(index).getId())) {
                return index;
            }
        }
        return Integer.MAX_VALUE;
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
