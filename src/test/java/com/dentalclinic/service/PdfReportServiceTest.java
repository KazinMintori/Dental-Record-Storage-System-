package com.dentalclinic.service;

import com.dentalclinic.model.report.ClinicInfo;
import com.dentalclinic.model.report.MedicalBookReportRow;
import com.dentalclinic.model.report.RevenueReportRow;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PdfReportServiceTest {

    private static final Path OUTPUT_DIRECTORY = Path.of("target", "pdf-test-output");
    private final PdfReportService service = new PdfReportService();

    @Test
    void medicalBookIsLandscapeAndPreservesVietnameseUnicode() throws Exception {
        Path output = OUTPUT_DIRECTORY.resolve("medical-sample.pdf");
        service.exportMedicalBook(output, List.of(new MedicalBookReportRow(
                        1, "Đỗ Minh Đức", "Nam", LocalDate.of(1995, 3, 12), "012345678901",
                        "BHYT-001", "Hà Nội", "Kỹ sư", "Kinh", "Đau răng hàm",
                        "Sâu răng", "Trám răng", "BS. Nguyễn Ánh", "Tái khám sau 7 ngày")),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        try (PDDocument document = Loader.loadPDF(output.toFile())) {
            assertTrue(document.getPage(0).getMediaBox().getWidth()
                    > document.getPage(0).getMediaBox().getHeight());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("SỔ KHÁM BỆNH"));
            assertTrue(text.contains("Đỗ Minh Đức"));
            assertTrue(text.contains("Phương pháp điều trị"));
        }
    }

    @Test
    void revenueBookIsPortraitAndContainsExactTotal() throws Exception {
        Path output = OUTPUT_DIRECTORY.resolve("revenue-sample.pdf");
        service.exportRevenueReport(output, List.of(
                        new RevenueReportRow("PT-01", LocalDate.of(2026, 8, 10),
                                "Khám và điều trị", new BigDecimal("100000.50")),
                        new RevenueReportRow(null, LocalDate.of(2026, 8, 11),
                                null, new BigDecimal("200000.25"))),
                new ClinicInfo("Nha khoa Ánh Dương", "Hà Nội", "0101234567"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        try (PDDocument document = Loader.loadPDF(output.toFile())) {
            assertTrue(document.getPage(0).getMediaBox().getWidth()
                    < document.getPage(0).getMediaBox().getHeight());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("SỔ DOANH THU BÁN HÀNG HÓA, DỊCH VỤ"));
            assertTrue(text.contains("HỘ, CÁ NHÂN KINH DOANH:"));
            assertTrue(text.contains("Mẫu số S2a-HKD"));
            assertTrue(text.contains("Thông tư số 152/2025/TT-BTC"));
            assertTrue(text.contains("Địa điểm kinh doanh:"));
            assertTrue(text.contains("Kỳ kê khai:"));
            assertTrue(text.contains("A B C 1"));
            assertTrue(text.contains("100.000,50"));
            assertTrue(text.contains("Tổng cộng"));
            assertTrue(text.contains("300.000,75"));
            assertTrue(text.contains("Thuế GTGT:"));
            assertTrue(text.contains("Thuế TNCN:"));
            assertTrue(text.contains("Ngày ... tháng ... năm ..."));
            assertTrue(text.contains("NGƯỜI ĐẠI DIỆN HỘ KINH DOANH"));
            assertFalse(text.contains("Nha khoa Ánh Dương"));
            assertFalse(text.contains("0101234567"));
        }
    }

    @Test
    void revenueBookRepeatsCompletePaperFormAndPeriodTotalOnEveryPage() throws Exception {
        Path output = OUTPUT_DIRECTORY.resolve("revenue-multiple-pages.pdf");
        List<RevenueReportRow> rows = new ArrayList<>();
        for (int index = 1; index <= 15; index++) {
            rows.add(new RevenueReportRow("PT-" + index, LocalDate.of(2026, 8, index),
                    "Điều trị " + index, new BigDecimal("100000")));
        }

        service.exportRevenueReport(output, rows,
                new ClinicInfo("Không in vào chỗ chấm", "Không in vào chỗ chấm", "Không in vào chỗ chấm"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        try (PDDocument document = Loader.loadPDF(output.toFile())) {
            assertTrue(document.getNumberOfPages() == 2);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= 2; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                assertTrue(text.contains("Mẫu số S2a-HKD"));
                assertTrue(text.contains("SỔ DOANH THU BÁN HÀNG HÓA, DỊCH VỤ"));
                assertTrue(text.contains("Tổng cộng:"));
                assertTrue(text.contains("1.500.000"));
                assertTrue(text.contains("Thuế GTGT:"));
                assertTrue(text.contains("Thuế TNCN:"));
                assertTrue(text.contains("NGƯỜI ĐẠI DIỆN HỘ KINH DOANH"));
            }
        }
    }

    @Test
    void emptyReportsStillContainHeadersAndBlankRows() throws Exception {
        Files.createDirectories(OUTPUT_DIRECTORY);
        Path medical = OUTPUT_DIRECTORY.resolve("medical-empty.pdf");
        Path revenue = OUTPUT_DIRECTORY.resolve("revenue-empty.pdf");
        LocalDate date = LocalDate.of(2026, 8, 14);

        service.exportMedicalBook(medical, List.of(), date, date);
        service.exportRevenueReport(revenue, List.of(),
                new ClinicInfo("PHÒNG KHÁM NHA KHOA", "Chưa cấu hình", "Chưa cấu hình"), date, date);

        try (PDDocument medicalDocument = Loader.loadPDF(medical.toFile());
             PDDocument revenueDocument = Loader.loadPDF(revenue.toFile())) {
            assertTrue(new PDFTextStripper().getText(medicalDocument).contains("Họ và tên"));
            String revenueText = new PDFTextStripper().getText(revenueDocument);
            assertTrue(revenueText.contains("Diễn giải"));
            assertTrue(revenueText.contains("Tổng cộng"));
            assertTrue(revenueText.contains("Thuế GTGT:"));
            assertTrue(revenueText.contains("Ngày ... tháng ... năm ..."));
            assertTrue(revenueText.contains("........"));
            assertFalse(revenueText.contains("Chưa cấu hình"));
        }
    }
}
