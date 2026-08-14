package com.dentalclinic.service;

import com.dentalclinic.model.report.MedicalBookReportRow;
import com.dentalclinic.model.report.RevenueReportRow;
import com.dentalclinic.repository.ReportRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportServiceTest {

    @Test
    void reportRangeUsesClinicDayAndExclusiveDayAfterEndDate() {
        CapturingRepository repository = new CapturingRepository();
        ReportService service = new ReportService(repository, ZoneId.of("Asia/Ho_Chi_Minh"));

        assertEquals(List.of(), service.getMedicalBook(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12)));

        assertEquals(OffsetDateTime.parse("2026-08-10T00:00:00+07:00"), repository.from);
        assertEquals(OffsetDateTime.parse("2026-08-13T00:00:00+07:00"), repository.to);
    }

    @Test
    void invalidRangeUsesRequiredVietnameseMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ReportService.validateDates(
                        LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 12)));

        assertEquals("Khoảng thời gian không hợp lệ", exception.getMessage());
    }

    @Test
    void emptyResultsAreValidAndRevenueTotalIsExact() {
        CapturingRepository repository = new CapturingRepository();
        ReportService service = new ReportService(repository, ZoneId.of("Asia/Ho_Chi_Minh"));

        assertEquals(List.of(), service.getRevenueReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1)));
        assertEquals(new BigDecimal("300.75"), ReportService.totalRevenue(List.of(
                new RevenueReportRow("A", LocalDate.of(2026, 8, 1), "Khám", new BigDecimal("100.50")),
                new RevenueReportRow("B", LocalDate.of(2026, 8, 1), "Điều trị", new BigDecimal("200.25"))
        )));
    }

    private static final class CapturingRepository extends ReportRepository {
        private OffsetDateTime from;
        private OffsetDateTime to;

        private CapturingRepository() {
            super(() -> null);
        }

        @Override
        public List<MedicalBookReportRow> findMedicalBookRows(
                OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
            from = fromInclusive;
            to = toExclusive;
            return List.of();
        }

        @Override
        public List<RevenueReportRow> findRevenueRows(
                OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
            from = fromInclusive;
            to = toExclusive;
            return List.of();
        }
    }
}
