package com.dentalclinic.repository;

import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.model.report.MedicalBookReportRow;
import com.dentalclinic.model.report.RevenueReportRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReportRepository {

    private static final String MEDICAL_BOOK_SQL = """
            SELECT ROW_NUMBER() OVER (ORDER BY v.created_at ASC, v.id ASC)::INTEGER AS tt,
                   p.ho_va_ten, p.gioi_tinh, p.ngay_sinh, p.giay_to_tuy_than,
                   p.so_the_bhyt, p.dia_chi, p.nghe_nghiep, p.dan_toc,
                   v.trieu_chung, v.chan_doan, v.phuong_phap_dieu_tri,
                   v.bac_si_kham, v.ghi_chu
            FROM visits v
            JOIN patients p ON p.id = v.patient_id
            WHERE v.created_at >= ? AND v.created_at < ?
            ORDER BY v.created_at ASC, v.id ASC
            """;

    private static final String REVENUE_SQL = """
            SELECT d.so_hieu, d.ngay_thang, d.dien_giai, d.so_tien
            FROM doanh_thu d
            JOIN visits v ON v.id = d.visit_id
            WHERE v.created_at >= ? AND v.created_at < ?
            ORDER BY v.created_at ASC, v.id ASC, d.id ASC
            """;

    private final ConnectionProvider connectionProvider;

    public ReportRepository() {
        this(() -> new DatabaseConfig().getConnection());
    }

    public ReportRepository(DatabaseConfig databaseConfig) {
        Objects.requireNonNull(databaseConfig, "databaseConfig must not be null");
        this.connectionProvider = databaseConfig::getConnection;
    }

    protected ReportRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    public List<MedicalBookReportRow> findMedicalBookRows(
            OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        validateRange(fromInclusive, toExclusive);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(MEDICAL_BOOK_SQL)) {
            bindRange(statement, fromInclusive, toExclusive);
            try (ResultSet results = statement.executeQuery()) {
                List<MedicalBookReportRow> rows = new ArrayList<>();
                while (results.next()) {
                    rows.add(new MedicalBookReportRow(
                            results.getInt("tt"), results.getString("ho_va_ten"),
                            results.getString("gioi_tinh"), results.getObject("ngay_sinh", java.time.LocalDate.class),
                            results.getString("giay_to_tuy_than"), results.getString("so_the_bhyt"),
                            results.getString("dia_chi"), results.getString("nghe_nghiep"),
                            results.getString("dan_toc"), results.getString("trieu_chung"),
                            results.getString("chan_doan"), results.getString("phuong_phap_dieu_tri"),
                            results.getString("bac_si_kham"), results.getString("ghi_chu")
                    ));
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load the medical-book report.", exception);
        }
    }

    public List<RevenueReportRow> findRevenueRows(
            OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        validateRange(fromInclusive, toExclusive);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(REVENUE_SQL)) {
            bindRange(statement, fromInclusive, toExclusive);
            try (ResultSet results = statement.executeQuery()) {
                List<RevenueReportRow> rows = new ArrayList<>();
                while (results.next()) {
                    rows.add(new RevenueReportRow(
                            results.getString("so_hieu"),
                            results.getObject("ngay_thang", java.time.LocalDate.class),
                            results.getString("dien_giai"),
                            results.getBigDecimal("so_tien")
                    ));
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not load the revenue report.", exception);
        }
    }

    private static void bindRange(
            PreparedStatement statement, OffsetDateTime fromInclusive, OffsetDateTime toExclusive) throws SQLException {
        statement.setObject(1, fromInclusive);
        statement.setObject(2, toExclusive);
    }

    private static void validateRange(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        Objects.requireNonNull(fromInclusive, "fromInclusive must not be null");
        Objects.requireNonNull(toExclusive, "toExclusive must not be null");
        if (!fromInclusive.isBefore(toExclusive)) {
            throw new IllegalArgumentException("The report time range must have a positive duration.");
        }
    }

    @FunctionalInterface
    protected interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
