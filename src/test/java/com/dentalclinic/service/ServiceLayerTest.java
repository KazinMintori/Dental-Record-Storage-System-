package com.dentalclinic.service;

import com.dentalclinic.model.Patient;
import com.dentalclinic.model.Revenue;
import com.dentalclinic.model.Visit;
import com.dentalclinic.repository.PatientRepository;
import com.dentalclinic.repository.RepositoryException;
import com.dentalclinic.repository.RevenueRepository;
import com.dentalclinic.repository.VisitRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceLayerTest {

    private final PatientService patientService = new PatientService(new PatientTestRepository());
    private final VisitService visitService = new VisitService(new VisitTestRepository());
    private final RevenueService revenueService = new RevenueService(new RevenueTestRepository());

    @Test
    void validPatientIsAccepted() {
        Patient patient = validPatient();
        assertSame(patient, patientService.createPatient(patient));
    }

    @Test
    void nullPatientIsRejected() {
        assertThrows(ServiceException.class, () -> patientService.createPatient(null));
    }

    @Test
    void missingPatientNameIsRejected() throws Exception {
        Patient patient = validPatient();
        setField(patient, "hoVaTen", null);
        assertThrows(ServiceException.class, () -> patientService.createPatient(patient));
    }

    @Test
    void blankPatientNameIsRejected() {
        Patient patient = validPatient();
        patient.setHoVaTen("   ");
        assertThrows(ServiceException.class, () -> patientService.createPatient(patient));
    }

    @Test
    void missingPatientGenderIsRejected() throws Exception {
        Patient patient = validPatient();
        setField(patient, "gioiTinh", null);
        assertThrows(ServiceException.class, () -> patientService.createPatient(patient));
    }

    @Test
    void missingPatientBirthDateIsRejected() throws Exception {
        Patient patient = validPatient();
        setField(patient, "ngaySinh", null);
        assertThrows(ServiceException.class, () -> patientService.createPatient(patient));
    }

    @Test
    void optionalPatientFieldsMayBeNull() {
        assertDoesNotThrow(() -> patientService.createPatient(validPatient()));
    }

    @Test
    void validVisitIsAccepted() {
        Visit visit = validVisit();
        assertSame(visit, visitService.createVisit(visit));
    }

    @Test
    void nullVisitIsRejected() {
        assertThrows(ServiceException.class, () -> visitService.createVisit(null));
    }

    @Test
    void missingRequiredVisitFieldIsRejected() throws Exception {
        Visit visit = validVisit();
        setField(visit, "patientId", null);
        assertThrows(ServiceException.class, () -> visitService.createVisit(visit));
    }

    @Test
    void blankRequiredVisitTextIsRejected() {
        Visit visit = validVisit();
        visit.setChanDoan("\t");
        assertThrows(ServiceException.class, () -> visitService.createVisit(visit));
    }

    @Test
    void nullVisitNoteIsAccepted() {
        Visit visit = validVisit();
        visit.setGhiChu(null);
        assertDoesNotThrow(() -> visitService.createVisit(visit));
    }

    @Test
    void invalidVisitDateRangeIsRejected() {
        LocalDate date = LocalDate.of(2026, 8, 13);
        assertThrows(ServiceException.class, () -> visitService.getVisitsByDateRange(null, date));
        assertThrows(ServiceException.class, () -> visitService.getVisitsByDateRange(date, null));
        assertThrows(ServiceException.class, () -> visitService.getVisitsByDateRange(date, date.minusDays(1)));
    }

    @Test
    void validRevenueIsAccepted() {
        Revenue revenue = validRevenue();
        assertSame(revenue, revenueService.createRevenue(revenue));
    }

    @Test
    void nullRevenueIsRejected() {
        assertThrows(ServiceException.class, () -> revenueService.createRevenue(null));
    }

    @Test
    void missingRequiredRevenueFieldIsRejected() throws Exception {
        Revenue revenue = validRevenue();
        setField(revenue, "visitId", null);
        assertThrows(ServiceException.class, () -> revenueService.createRevenue(revenue));
    }

    @Test
    void blankRevenueDescriptionIsRejected() {
        Revenue revenue = validRevenue();
        revenue.setDienGiai("  ");
        assertThrows(ServiceException.class, () -> revenueService.createRevenue(revenue));
    }

    @Test
    void nullRevenueAmountIsRejected() throws Exception {
        Revenue revenue = validRevenue();
        setField(revenue, "soTien", null);
        assertThrows(ServiceException.class, () -> revenueService.createRevenue(revenue));
    }

    @Test
    void negativeRevenueAmountIsRejected() {
        Revenue revenue = validRevenue();
        revenue.setSoTien(new BigDecimal("-0.01"));
        assertThrows(ServiceException.class, () -> revenueService.createRevenue(revenue));
    }

    @Test
    void nullSoHieuIsAccepted() {
        Revenue revenue = validRevenue();
        revenue.setSoHieu(null);
        assertDoesNotThrow(() -> revenueService.createRevenue(revenue));
    }

    @Test
    void invalidRevenueDateRangeIsRejected() {
        LocalDate date = LocalDate.of(2026, 8, 13);
        assertThrows(ServiceException.class, () -> revenueService.getRevenueByDateRange(null, date));
        assertThrows(ServiceException.class, () -> revenueService.calculateRevenueTotal(date, null));
        assertThrows(ServiceException.class, () -> revenueService.getRevenueByDateRange(date, date.minusDays(1)));
    }

    @Test
    void revenueTotalUsesBigDecimal() {
        BigDecimal total = revenueService.calculateRevenueTotal(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        assertEquals(new BigDecimal("1234.56"), total);
    }

    @Test
    void repositoryExceptionsAreTranslatedAndCauseIsPreserved() {
        RepositoryException cause = new RepositoryException("database detail");
        PatientService failingService = new PatientService(new PatientTestRepository() {
            @Override
            public Patient save(Patient patient) {
                throw cause;
            }
        });

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> failingService.createPatient(validPatient())
        );
        assertSame(cause, exception.getCause());
        assertEquals("Unable to save patient.", exception.getMessage());
    }

    @Test
    void nullIdsAreRejectedConsistently() {
        assertThrows(ServiceException.class, () -> patientService.getPatient(null));
        assertThrows(ServiceException.class, () -> visitService.getVisit(null));
        assertThrows(ServiceException.class, () -> revenueService.getRevenue(null));
    }

    private static Patient validPatient() {
        return new Patient("Nguyen Van An", "Nam", LocalDate.of(1990, 1, 2));
    }

    private static Visit validVisit() {
        return new Visit(1L, 1, LocalDate.of(2026, 8, 13),
                "Dau rang", "Sau rang", "Tram rang", "Bac si An");
    }

    private static Revenue validRevenue() {
        return new Revenue(1L, LocalDate.of(2026, 8, 13),
                "Thanh toan dieu tri", new BigDecimal("500000.00"));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class PatientTestRepository extends PatientRepository {
        @Override
        public Patient save(Patient patient) {
            return patient;
        }
    }

    private static class VisitTestRepository extends VisitRepository {
        @Override
        public Visit save(Visit visit) {
            return visit;
        }
    }

    private static class RevenueTestRepository extends RevenueRepository {
        @Override
        public Revenue save(Revenue revenue) {
            return revenue;
        }

        @Override
        public BigDecimal calculateTotalByDateRange(LocalDate from, LocalDate to) {
            return new BigDecimal("1234.56");
        }
    }
}
