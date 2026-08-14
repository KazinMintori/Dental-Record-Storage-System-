package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;
import com.dentalclinic.model.Visit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitRepositoryTest {

    private Connection transactionConnection;
    private PatientRepository patientRepository;
    private VisitRepository visitRepository;

    @BeforeEach
    void beginTransaction() throws Exception {
        transactionConnection = new DatabaseConfig().getConnection();
        transactionConnection.setAutoCommit(false);
        Connection nonClosingConnection = nonClosing(transactionConnection);
        patientRepository = new PatientRepository(() -> nonClosingConnection);
        visitRepository = new VisitRepository(() -> nonClosingConnection);
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
    void saveCreatesVisit() {
        Patient patient = saveTemporaryPatient();
        Visit visit = completeVisit(patient.getId(), LocalDate.of(2026, 8, 10));

        Visit saved = visitRepository.save(visit);
        Visit loaded = visitRepository.findById(saved.getId());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertVisitFieldsEqual(visit, loaded);
    }

    @Test
    void saveCreatesMultipleVisitsForSamePatient() {
        Patient patient = saveTemporaryPatient();

        Visit first = visitRepository.save(completeVisit(patient.getId(), LocalDate.of(2026, 8, 1)));
        Visit second = visitRepository.save(completeVisit(patient.getId(), LocalDate.of(2026, 8, 10)));

        assertNotEquals(first.getId(), second.getId());
        List<Visit> visits = visitRepository.findByPatientId(patient.getId());
        assertEquals(List.of(first.getId(), second.getId()), visits.stream().map(Visit::getId).toList());
    }

    @Test
    void findByIdReturnsVisit() {
        Visit saved = visitRepository.save(completeVisit(saveTemporaryPatient().getId(), LocalDate.of(2026, 8, 12)));

        Visit found = visitRepository.findById(saved.getId());

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertVisitFieldsEqual(saved, found);
        assertEquals(saved.getCreatedAt(), found.getCreatedAt());
        assertEquals(saved.getUpdatedAt(), found.getUpdatedAt());
    }

    @Test
    void findByIdReturnsNullForMissingId() {
        assertNull(visitRepository.findById(Long.MAX_VALUE));
    }

    @Test
    void findByPatientIdReturnsChronologicalVisits() {
        Long patientId = saveTemporaryPatient().getId();
        Visit latest = visitRepository.save(completeVisit(patientId, LocalDate.of(2026, 8, 20)));
        Visit sameDayFirst = visitRepository.save(completeVisit(patientId, LocalDate.of(2026, 8, 10)));
        Visit earliest = visitRepository.save(completeVisit(patientId, LocalDate.of(2026, 8, 1)));
        Visit sameDaySecond = visitRepository.save(completeVisit(patientId, LocalDate.of(2026, 8, 10)));

        List<Long> orderedIds = visitRepository.findByPatientId(patientId).stream().map(Visit::getId).toList();

        assertEquals(List.of(earliest.getId(), sameDayFirst.getId(), sameDaySecond.getId(), latest.getId()), orderedIds);
    }

    @Test
    void findByDateRangeReturnsInclusiveResults() {
        Long patientId = saveTemporaryPatient().getId();
        LocalDate from = LocalDate.of(2098, 4, 10);
        LocalDate to = LocalDate.of(2098, 4, 20);
        Visit before = visitRepository.save(completeVisit(patientId, from.minusDays(1)));
        Visit start = visitRepository.save(completeVisit(patientId, from));
        Visit inside = visitRepository.save(completeVisit(patientId, from.plusDays(5)));
        Visit end = visitRepository.save(completeVisit(patientId, to));
        Visit after = visitRepository.save(completeVisit(patientId, to.plusDays(1)));

        Set<Long> testIds = Set.of(before.getId(), start.getId(), inside.getId(), end.getId(), after.getId());
        List<Long> matchingTestIds = visitRepository.findByDateRange(from, to).stream()
                .map(Visit::getId)
                .filter(testIds::contains)
                .toList();

        assertEquals(List.of(start.getId(), inside.getId(), end.getId()), matchingTestIds);
    }

    @Test
    void findByDateRangeRejectsInvalidRange() {
        LocalDate from = LocalDate.of(2026, 8, 20);
        LocalDate to = LocalDate.of(2026, 8, 10);

        assertThrows(IllegalArgumentException.class, () -> visitRepository.findByDateRange(from, to));
    }

    @Test
    void updateChangesVisit() throws Exception {
        Patient originalPatient = saveTemporaryPatient();
        Visit saved = visitRepository.save(completeVisit(originalPatient.getId(), LocalDate.of(2026, 8, 10)));
        makeUpdatedTimestampOlder(saved.getId());
        OffsetDateTime previousUpdatedAt = visitRepository.findById(saved.getId()).getUpdatedAt();

        saved.setTt(99);
        saved.setNgayKham(LocalDate.of(2026, 9, 1));
        saved.setTrieuChung("Trieu chung da cap nhat");
        saved.setChanDoan("Chan doan da cap nhat");
        saved.setPhuongPhapDieuTri("Dieu tri da cap nhat");
        saved.setBacSiKham("BS. Cap Nhat");
        saved.setGhiChu("Ghi chu da cap nhat");
        visitRepository.update(saved);

        Visit updated = visitRepository.findById(saved.getId());
        assertVisitFieldsEqual(saved, updated);
        assertTrue(updated.getUpdatedAt().isAfter(previousUpdatedAt));
        assertEquals(saved.getCreatedAt(), updated.getCreatedAt());
    }

    @Test
    void updateCannotMoveVisitToAnotherPatientRecord() {
        Patient originalPatient = saveTemporaryPatient();
        Patient otherPatient = saveTemporaryPatient();
        Visit saved = visitRepository.save(completeVisit(originalPatient.getId(), LocalDate.of(2026, 8, 10)));

        saved.setPatientId(otherPatient.getId());

        assertThrows(RepositoryException.class, () -> visitRepository.update(saved));
        assertEquals(originalPatient.getId(), visitRepository.findById(saved.getId()).getPatientId());
    }

    @Test
    void optionalGhiChuCanBeNull() {
        Visit visit = new Visit(
                saveTemporaryPatient().getId(), 1, LocalDate.of(2026, 8, 13),
                "Dau rang", "Sau rang", "Tram rang", "BS. Lan"
        );

        Visit saved = visitRepository.save(visit);
        Visit loaded = visitRepository.findById(saved.getId());

        assertNull(loaded.getGhiChu());
    }

    @Test
    void deleteRemovesVisit() {
        Visit saved = visitRepository.save(completeVisit(saveTemporaryPatient().getId(), LocalDate.of(2026, 8, 13)));

        visitRepository.delete(saved.getId());

        assertNull(visitRepository.findById(saved.getId()));
    }

    @Test
    void invalidPatientIdIsRejected() {
        Visit visit = completeVisit(Long.MAX_VALUE, LocalDate.of(2026, 8, 13));

        RepositoryException exception = assertThrows(RepositoryException.class, () -> visitRepository.save(visit));

        assertInstanceOf(SQLException.class, exception.getCause());
    }

    private Patient saveTemporaryPatient() {
        return patientRepository.save(new Patient(
                "Phase5 Test " + UUID.randomUUID(), "Nam", LocalDate.of(1990, 1, 2)
        ));
    }

    private static Visit completeVisit(Long patientId, LocalDate date) {
        Visit visit = new Visit(
                patientId, 1, date, "Dau rang", "Sau rang", "Tram rang", "BS. Lan"
        );
        visit.setGhiChu("Ghi chu " + UUID.randomUUID());
        return visit;
    }

    private void makeUpdatedTimestampOlder(Long visitId) throws Exception {
        try (PreparedStatement statement = transactionConnection.prepareStatement("""
                UPDATE visits
                SET updated_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
                WHERE id = ?
                """)) {
            statement.setLong(1, visitId);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void assertVisitFieldsEqual(Visit expected, Visit actual) {
        assertNotNull(actual);
        assertEquals(expected.getPatientId(), actual.getPatientId());
        assertEquals(expected.getTt(), actual.getTt());
        assertEquals(expected.getNgayKham(), actual.getNgayKham());
        assertEquals(expected.getTrieuChung(), actual.getTrieuChung());
        assertEquals(expected.getChanDoan(), actual.getChanDoan());
        assertEquals(expected.getPhuongPhapDieuTri(), actual.getPhuongPhapDieuTri());
        assertEquals(expected.getBacSiKham(), actual.getBacSiKham());
        assertEquals(expected.getGhiChu(), actual.getGhiChu());
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
