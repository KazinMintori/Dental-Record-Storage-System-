package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;
import com.dentalclinic.model.Revenue;
import com.dentalclinic.model.Visit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RevenueRepositoryTest {

    private Connection transactionConnection;
    private PatientRepository patientRepository;
    private VisitRepository visitRepository;
    private RevenueRepository revenueRepository;

    @BeforeEach
    void beginTransaction() throws Exception {
        transactionConnection = new DatabaseConfig().getConnection();
        transactionConnection.setAutoCommit(false);
        Connection connection = nonClosing(transactionConnection);
        patientRepository = new PatientRepository(() -> connection);
        visitRepository = new VisitRepository(() -> connection);
        revenueRepository = new RevenueRepository(() -> connection);
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
    void saveCreatesRevenue() {
        Revenue revenue = completeRevenue(saveTemporaryVisit().getId(), LocalDate.of(9000, 1, 1), "1234567.89");

        Revenue saved = revenueRepository.save(revenue);
        Revenue loaded = revenueRepository.findById(saved.getId());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertRevenueFieldsEqual(revenue, loaded);
        assertEquals(new BigDecimal("1234567.89"), loaded.getSoTien());
    }

    @Test
    void saveAllowsNullSoHieu() {
        Revenue revenue = new Revenue(
                saveTemporaryVisit().getId(), LocalDate.of(9000, 1, 2), "Kham rang", new BigDecimal("100.00")
        );

        Revenue saved = revenueRepository.save(revenue);

        assertNull(revenueRepository.findById(saved.getId()).getSoHieu());
    }

    @Test
    void findByIdReturnsRevenue() {
        Revenue saved = revenueRepository.save(
                completeRevenue(saveTemporaryVisit().getId(), LocalDate.of(9000, 1, 3), "250.25")
        );

        Revenue found = revenueRepository.findById(saved.getId());

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertRevenueFieldsEqual(saved, found);
        assertEquals(saved.getCreatedAt(), found.getCreatedAt());
    }

    @Test
    void findByIdReturnsNullForMissingId() {
        assertNull(revenueRepository.findById(Long.MAX_VALUE));
    }

    @Test
    void findByVisitIdReturnsMultipleRevenueEntries() {
        Long visitId = saveTemporaryVisit().getId();
        Revenue latest = revenueRepository.save(completeRevenue(visitId, LocalDate.of(9000, 2, 20), "300.00"));
        Revenue sameDayFirst = revenueRepository.save(completeRevenue(visitId, LocalDate.of(9000, 2, 10), "100.00"));
        Revenue earliest = revenueRepository.save(completeRevenue(visitId, LocalDate.of(9000, 2, 1), "50.00"));
        Revenue sameDaySecond = revenueRepository.save(completeRevenue(visitId, LocalDate.of(9000, 2, 10), "200.00"));

        List<Long> ids = revenueRepository.findByVisitId(visitId).stream().map(Revenue::getId).toList();

        assertEquals(List.of(earliest.getId(), sameDayFirst.getId(), sameDaySecond.getId(), latest.getId()), ids);
        assertNotEquals(sameDayFirst.getId(), sameDaySecond.getId());
    }

    @Test
    void findByDateRangeReturnsInclusiveResults() {
        Long visitId = saveTemporaryVisit().getId();
        LocalDate from = LocalDate.of(9000, 3, 10);
        LocalDate to = LocalDate.of(9000, 3, 20);
        Revenue before = revenueRepository.save(completeRevenue(visitId, from.minusDays(1), "10.00"));
        Revenue start = revenueRepository.save(completeRevenue(visitId, from, "20.00"));
        Revenue inside = revenueRepository.save(completeRevenue(visitId, from.plusDays(5), "30.00"));
        Revenue end = revenueRepository.save(completeRevenue(visitId, to, "40.00"));
        Revenue after = revenueRepository.save(completeRevenue(visitId, to.plusDays(1), "50.00"));
        Set<Long> testIds = Set.of(before.getId(), start.getId(), inside.getId(), end.getId(), after.getId());

        List<Long> matchingIds = revenueRepository.findByDateRange(from, to).stream()
                .map(Revenue::getId).filter(testIds::contains).toList();

        assertEquals(List.of(start.getId(), inside.getId(), end.getId()), matchingIds);
    }

    @Test
    void findByDateRangeRejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> revenueRepository.findByDateRange(
                LocalDate.of(9000, 4, 20), LocalDate.of(9000, 4, 10)
        ));
    }

    @Test
    void calculateTotalReturnsExactBigDecimal() {
        Long visitId = saveTemporaryVisit().getId();
        LocalDate date = LocalDate.of(9000, 5, 1);
        revenueRepository.save(completeRevenue(visitId, date, "100.50"));
        revenueRepository.save(completeRevenue(visitId, date.plusDays(1), "200.25"));
        revenueRepository.save(completeRevenue(visitId, date.plusDays(2), "99.25"));

        BigDecimal total = revenueRepository.calculateTotalByDateRange(date, date.plusDays(2));

        assertEquals(new BigDecimal("400.00"), total);
    }

    @Test
    void calculateTotalReturnsZeroForEmptyRange() {
        BigDecimal total = revenueRepository.calculateTotalByDateRange(
                LocalDate.of(9998, 1, 1), LocalDate.of(9998, 1, 2)
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void updateChangesRevenue() {
        Visit originalVisit = saveTemporaryVisit();
        Visit newVisit = saveTemporaryVisit();
        Revenue saved = revenueRepository.save(
                completeRevenue(originalVisit.getId(), LocalDate.of(9000, 6, 1), "100.00")
        );

        saved.setVisitId(newVisit.getId());
        saved.setSoHieu("UPDATED-001");
        saved.setNgayThang(LocalDate.of(9000, 6, 2));
        saved.setDienGiai("Dien giai da cap nhat");
        saved.setSoTien(new BigDecimal("987654.32"));
        revenueRepository.update(saved);

        Revenue updated = revenueRepository.findById(saved.getId());
        assertRevenueFieldsEqual(saved, updated);
        assertEquals(saved.getCreatedAt(), updated.getCreatedAt());
    }

    @Test
    void deleteRemovesRevenue() {
        Revenue saved = revenueRepository.save(
                completeRevenue(saveTemporaryVisit().getId(), LocalDate.of(9000, 7, 1), "100.00")
        );

        revenueRepository.delete(saved.getId());

        assertNull(revenueRepository.findById(saved.getId()));
    }

    @Test
    void invalidVisitIdIsRejected() {
        Revenue revenue = completeRevenue(Long.MAX_VALUE, LocalDate.of(9000, 8, 1), "100.00");

        RepositoryException exception = assertThrows(RepositoryException.class, () -> revenueRepository.save(revenue));

        assertInstanceOf(SQLException.class, exception.getCause());
    }

    private Visit saveTemporaryVisit() {
        Patient patient = patientRepository.save(new Patient(
                "Phase6 Test " + UUID.randomUUID(), "Nam", LocalDate.of(1990, 1, 2)
        ));
        return visitRepository.save(new Visit(
                patient.getId(), 1, LocalDate.of(2026, 8, 13),
                "Dau rang", "Sau rang", "Tram rang", "BS. Lan"
        ));
    }

    private static Revenue completeRevenue(Long visitId, LocalDate date, String amount) {
        Revenue revenue = new Revenue(visitId, date, "Phase6 Revenue", new BigDecimal(amount));
        revenue.setSoHieu("P6-" + UUID.randomUUID());
        return revenue;
    }

    private static void assertRevenueFieldsEqual(Revenue expected, Revenue actual) {
        assertNotNull(actual);
        assertEquals(expected.getVisitId(), actual.getVisitId());
        assertEquals(expected.getSoHieu(), actual.getSoHieu());
        assertEquals(expected.getNgayThang(), actual.getNgayThang());
        assertEquals(expected.getDienGiai(), actual.getDienGiai());
        assertEquals(expected.getSoTien(), actual.getSoTien());
    }

    private static Connection nonClosing(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
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
