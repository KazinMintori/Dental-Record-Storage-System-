package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.Patient;
import com.dentalclinic.model.Revenue;
import com.dentalclinic.model.Visit;
import com.dentalclinic.service.ServiceException;
import com.dentalclinic.service.VisitRevenueWorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitRevenueWorkflowTest {

    private Connection connection;
    private PatientRepository patients;
    private VisitRepository visits;
    private RevenueRepository revenues;
    private VisitRevenueWorkflowService workflow;
    private Long patientId;

    @BeforeEach
    void beginTransaction() throws Exception {
        connection = new DatabaseConfig().getConnection();
        connection.setAutoCommit(false);
        Connection shared = transactionControlled(connection);
        patients = new PatientRepository(() -> shared);
        visits = new VisitRepository(() -> shared);
        revenues = new RevenueRepository(() -> shared);
        workflow = new VisitRevenueWorkflowService(new RepositoryTransaction(() -> shared));
        patientId = patients.save(new Patient(
                "Phase10 " + UUID.randomUUID(), "Nam", LocalDate.of(1990, 1, 2))).getId();
    }

    @AfterEach
    void rollback() throws Exception {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    void createsMultipleVisitsAndAllowsNoRevenue() {
        Visit first = workflow.create(visit(1), List.of()).visit();
        Visit second = workflow.create(visit(2), List.of()).visit();

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, visits.findByPatientId(patientId).size());
        assertTrue(revenues.findByVisitId(first.getId()).isEmpty());
    }

    @Test
    void createsOneOrMultipleRevenueRowsForCorrectVisit() {
        Revenue first = revenue("Điều trị", "100.50");
        Revenue second = revenue("Điều trị bổ sung", "200.25");

        Visit savedVisit = workflow.create(visit(1), List.of(first, second)).visit();
        List<Revenue> saved = revenues.findByVisitId(savedVisit.getId());

        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(item -> item.getVisitId().equals(savedVisit.getId())));
        assertEquals(new BigDecimal("300.75"), saved.stream()
                .map(Revenue::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void rejectsInvalidVisitAndRevenueFields() {
        Visit invalidVisit = visit(1);
        invalidVisit.setChanDoan(" ");
        assertThrows(ServiceException.class, () -> workflow.create(invalidVisit, List.of()));

        Revenue negative = revenue("Điều trị", "-1.00");
        assertThrows(ServiceException.class, () -> workflow.create(visit(2), List.of(negative)));
    }

    @Test
    void failedRevenueSaveRollsBackCreatedVisit() {
        int before = visits.findByPatientId(patientId).size();

        assertThrows(ServiceException.class,
                () -> workflow.create(visit(7), List.of(revenue("Điều trị", "-10.00"))));

        assertEquals(before, visits.findByPatientId(patientId).size());
    }

    @Test
    void loadsAndEditsVisitWithoutDuplicateAndSynchronizesRevenue() {
        Visit savedVisit = workflow.create(visit(1), List.of(
                revenue("Trám răng", "100.00"), revenue("Thuốc", "50.00"))).visit();
        List<Revenue> existing = revenues.findByVisitId(savedVisit.getId());

        savedVisit.setChanDoan("Đã cập nhật");
        Revenue retained = existing.getFirst();
        retained.setDienGiai("Mô tả chỉnh sửa thủ công");
        retained.setSoTien(new BigDecimal("125.00"));
        Revenue added = revenue("Tái khám", "25.00");
        workflow.update(savedVisit, List.of(retained, added));

        assertEquals(1, visits.findByPatientId(patientId).size());
        assertEquals("Đã cập nhật", visits.findById(savedVisit.getId()).getChanDoan());
        List<Revenue> updated = revenues.findByVisitId(savedVisit.getId());
        assertEquals(2, updated.size());
        assertEquals("Mô tả chỉnh sửa thủ công", updated.getFirst().getDienGiai());
        assertNull(revenues.findById(existing.get(1).getId()));
    }

    @Test
    void deletingVisitUsesCascadeForRevenue() {
        Visit saved = workflow.create(visit(1), List.of(revenue("Điều trị", "100.00"))).visit();
        Long revenueId = revenues.findByVisitId(saved.getId()).getFirst().getId();

        workflow.delete(saved.getId());

        assertNull(visits.findById(saved.getId()));
        assertNull(revenues.findById(revenueId));
    }

    private Visit visit(int sequence) {
        return new Visit(patientId, sequence, LocalDate.of(2026, 8, sequence),
                "Đau răng", "Sâu răng", "Trám răng", "BS. An");
    }

    private Revenue revenue(String description, String amount) {
        return new Revenue(0L, LocalDate.of(2026, 8, 13), description, new BigDecimal(amount));
    }

    private static Connection transactionControlled(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("close") || method.getName().equals("commit")) {
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
