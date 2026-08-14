package com.dentalclinic.service;

import com.dentalclinic.model.report.MedicalBookReportRow;
import com.dentalclinic.model.report.RevenueReportRow;
import com.dentalclinic.repository.ReportRepository;
import com.dentalclinic.repository.RepositoryException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public class ReportService {

    private final ReportRepository reportRepository;
    private final ZoneId reportZone;

    public ReportService(ReportRepository reportRepository) {
        this(reportRepository, ZoneId.systemDefault());
    }

    public ReportService(ReportRepository reportRepository, ZoneId reportZone) {
        this.reportRepository = Objects.requireNonNull(reportRepository, "reportRepository must not be null");
        this.reportZone = Objects.requireNonNull(reportZone, "reportZone must not be null");
    }

    public List<MedicalBookReportRow> getMedicalBook(LocalDate from, LocalDate to) {
        TimeRange range = timeRange(from, to);
        try {
            return reportRepository.findMedicalBookRows(range.fromInclusive(), range.toExclusive());
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load the medical-book report.", exception);
        }
    }

    public List<RevenueReportRow> getRevenueReport(LocalDate from, LocalDate to) {
        TimeRange range = timeRange(from, to);
        try {
            return reportRepository.findRevenueRows(range.fromInclusive(), range.toExclusive());
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load the revenue report.", exception);
        }
    }

    public static BigDecimal totalRevenue(List<RevenueReportRow> rows) {
        Objects.requireNonNull(rows, "rows must not be null");
        return rows.stream().map(RevenueReportRow::amount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public TimeRange timeRange(LocalDate from, LocalDate to) {
        validateDates(from, to);
        return new TimeRange(
                from.atStartOfDay(reportZone).toOffsetDateTime(),
                to.plusDays(1).atStartOfDay(reportZone).toOffsetDateTime()
        );
    }

    public static void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ Từ ngày và Đến ngày");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        }
    }

    public record TimeRange(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
    }
}
