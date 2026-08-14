package com.dentalclinic.service;

import com.dentalclinic.model.report.ClinicInfo;
import com.dentalclinic.model.report.MedicalBookReportRow;
import com.dentalclinic.model.report.RevenueReportRow;
import com.dentalclinic.util.DatePickerSupport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PdfReportService {

    private static final PDRectangle A4_LANDSCAPE =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float BLACK = 0f;
    private static final float CELL_PADDING = 2.5f;
    private static final int REVENUE_ROWS_PER_PAGE = 14;
    private static final float REVENUE_BODY_ROW_HEIGHT = 25f;
    private static final float REVENUE_FOOTER_ROW_HEIGHT = 20f;
    private static final float REVENUE_DATA_FONT_SIZE = 9.3f;
    private static final float REVENUE_GRID_LINE_WIDTH = .9f;
    private static final float MEDICAL_GRID_LINE_WIDTH = .9f;

    public void exportMedicalBook(
            Path output,
            List<MedicalBookReportRow> sourceRows,
            LocalDate from,
            LocalDate to
    ) throws IOException {
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(sourceRows, "sourceRows must not be null");
        ReportService.validateDates(from, to);
        createParentDirectory(output);

        try (PDDocument document = new PDDocument()) {
            PDFont regular = loadFont(document, false);
            PDFont bold = loadFont(document, true);
            List<List<String>> rows = sourceRows.stream().map(this::medicalValues).toList();
            if (rows.isEmpty()) {
                rows = blankRows(14, 10);
            }

            float[] widths = scaledWidths(A4_LANDSCAPE.getWidth() - 36,
                    .035f, .10f, .05f, .065f, .08f, .065f, .095f,
                    .06f, .045f, .09f, .09f, .11f, .07f, .045f);
            List<String> headers = List.of(
                    "TT", "Họ và tên", "Giới tính", "Ngày tháng năm (sinh)",
                    "ĐDCN/Giấy tờ tuỳ thân", "Số thẻ BHYT", "Địa chỉ", "Nghề nghiệp",
                    "Dân tộc", "Triệu chứng", "Chẩn đoán", "Phương pháp điều trị",
                    "Y,BS khám bệnh", "Ghi chú"
            );

            PDPageContentStream content = null;
            float y = 0;
            int pageNumber = 0;
            try {
                for (List<String> row : rows) {
                    float rowHeight = rowHeight(row, widths, regular, 5.2f, 6.4f, 12);
                    if (content == null || y - rowHeight < 24) {
                        if (content != null) {
                            content.close();
                        }
                        PDPage page = new PDPage(A4_LANDSCAPE);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        pageNumber++;
                        y = drawMedicalPageHeader(content, regular, bold, headers, widths, from, to, pageNumber);
                    }
                    y = drawGridRow(content, 18, y, widths, row, rowHeight, regular, 5.2f, 6.4f, false, -1);
                }
            } finally {
                if (content != null) {
                    content.close();
                }
            }
            document.save(output.toFile());
        }
    }

    public void exportRevenueReport(
            Path output,
            List<RevenueReportRow> sourceRows,
            ClinicInfo clinicInfo,
            LocalDate from,
            LocalDate to
    ) throws IOException {
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(sourceRows, "sourceRows must not be null");
        Objects.requireNonNull(clinicInfo, "clinicInfo must not be null");
        ReportService.validateDates(from, to);
        createParentDirectory(output);

        try (PDDocument document = new PDDocument()) {
            PDFont regular = loadFont(document, false);
            PDFont bold = loadFont(document, true);
            PDFont italic = loadItalicFont(document);
            float[] widths = scaledWidths(PDRectangle.A4.getWidth() - 72, .18f, .20f, .42f, .20f);
            int pageCount = Math.max(1,
                    (sourceRows.size() + REVENUE_ROWS_PER_PAGE - 1) / REVENUE_ROWS_PER_PAGE);
            BigDecimal total = ReportService.totalRevenue(sourceRows);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                int fromIndex = pageIndex * REVENUE_ROWS_PER_PAGE;
                int toIndex = Math.min(sourceRows.size(), fromIndex + REVENUE_ROWS_PER_PAGE);
                List<RevenueReportRow> pageRows = fromIndex < toIndex
                        ? sourceRows.subList(fromIndex, toIndex) : List.of();
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    drawRevenuePaperPage(content, regular, bold, italic, widths, pageRows, total);
                }
            }
            document.save(output.toFile());
        }
    }

    private float drawMedicalPageHeader(
            PDPageContentStream content,
            PDFont regular,
            PDFont bold,
            List<String> headers,
            float[] widths,
            LocalDate from,
            LocalDate to,
            int pageNumber
    ) throws IOException {
        float pageWidth = A4_LANDSCAPE.getWidth();
        drawCenteredText(content, bold, 14, "SỔ KHÁM BỆNH", pageWidth, 565);
        drawCenteredText(content, regular, 8,
                "Từ ngày " + DatePickerSupport.format(from) + " đến ngày " + DatePickerSupport.format(to),
                pageWidth, 549);
        drawRightText(content, regular, 7, "Trang " + pageNumber, pageWidth - 18, 549);
        float headerHeight = Math.max(30, rowHeight(headers, widths, bold, 5.2f, 6.2f, 8));
        return drawGridRow(content, 18, 538, widths, headers, headerHeight, bold, 5.2f, 6.2f, true, -1);
    }

    private void drawRevenuePaperPage(
            PDPageContentStream content,
            PDFont regular,
            PDFont bold,
            PDFont italic,
            float[] widths,
            List<RevenueReportRow> rows,
            BigDecimal periodTotal
    ) throws IOException {
        String businessLine = "HỘ, CÁ NHÂN KINH DOANH: ......";
        drawText(content, bold, 9.5f, businessLine, 53, 799);
        drawText(content, bold, 9.5f,
                dotsMatchingWidth(bold, 9.5f, stringWidth(bold, 9.5f, businessLine)), 53, 783);
        drawText(content, bold, 9.5f,
                "Mã số thuế:........................................", 53, 761);
        drawText(content, bold, 9.5f,
                "Địa chỉ:................................................", 53, 745);

        drawCenteredTextInArea(content, bold, 9.2f, "Mẫu số S2a-HKD", 382, 177, 801);
        drawCenteredTextInArea(content, italic, 7.9f,
                "(Kèm theo Thông tư số 152/2025/TT-BTC", 382, 177, 787);
        drawCenteredTextInArea(content, italic, 7.9f,
                "ngày 31 tháng 12 năm 2025 của Bộ trưởng", 382, 177, 775);
        drawCenteredTextInArea(content, italic, 7.9f,
                "Bộ Tài chính)", 382, 177, 763);

        drawCenteredText(content, bold, 14.5f,
                "SỔ DOANH THU BÁN HÀNG HÓA, DỊCH VỤ", PDRectangle.A4.getWidth(), 716);
        drawCenteredText(content, regular, 10.5f,
                "Địa điểm kinh doanh:.................................",
                PDRectangle.A4.getWidth(), 697);
        drawCenteredText(content, regular, 10.5f,
                "Kỳ kê khai:................................................",
                PDRectangle.A4.getWidth(), 679);

        float x = 36;
        float y = drawRevenuePaperHeader(content, x, 650, widths, bold);
        for (int index = 0; index < REVENUE_ROWS_PER_PAGE; index++) {
            List<String> values = index < rows.size()
                    ? revenueValues(rows.get(index)) : List.of("", "", "", "");
            y = drawRevenuePaperRow(content, x, y, widths, values, regular);
        }
        drawRevenuePaperTotals(content, x, y, widths, periodTotal, regular, bold);

        drawCenteredTextInArea(content, italic, 9f,
                "Ngày ... tháng ... năm ...", 335, 190, 132);
        drawCenteredTextInArea(content, bold, 9.2f,
                "NGƯỜI ĐẠI DIỆN HỘ KINH DOANH", 335, 190, 115);
        drawCenteredTextInArea(content, italic, 8.8f,
                "(Ký, họ tên, đóng dấu)", 335, 190, 98);
    }

    private float drawRevenuePaperHeader(
            PDPageContentStream content,
            float x,
            float y,
            float[] widths,
            PDFont bold
    ) throws IOException {
        float upperHeight = 23;
        float lowerHeight = 23;
        float codeHeight = 19;
        float firstGroupWidth = widths[0] + widths[1];
        drawPaperCell(content, x, y, firstGroupWidth, upperHeight,
                "Chứng từ", bold, 8.5f, true, false);
        drawPaperCell(content, x, y - upperHeight, widths[0], lowerHeight,
                "Số hiệu", bold, 8, true, false);
        drawPaperCell(content, x + widths[0], y - upperHeight, widths[1], lowerHeight,
                "Ngày, tháng", bold, 8, true, false);
        drawPaperCell(content, x + firstGroupWidth, y, widths[2], upperHeight + lowerHeight,
                "Diễn giải", bold, 8.5f, true, false);
        drawPaperCell(content, x + firstGroupWidth + widths[2], y, widths[3], upperHeight + lowerHeight,
                "Số tiền", bold, 8.5f, true, false);
        float codeY = y - upperHeight - lowerHeight;
        String[] codes = {"A", "B", "C", "1"};
        float cellX = x;
        for (int index = 0; index < widths.length; index++) {
            drawPaperCell(content, cellX, codeY, widths[index], codeHeight,
                    codes[index], bold, 8.7f, true, false);
            cellX += widths[index];
        }
        return codeY - codeHeight;
    }

    private float drawRevenuePaperRow(
            PDPageContentStream content,
            float x,
            float y,
            float[] widths,
            List<String> values,
            PDFont regular
    ) throws IOException {
        float cellX = x;
        for (int index = 0; index < widths.length; index++) {
            drawPaperCell(content, cellX, y, widths[index], REVENUE_BODY_ROW_HEIGHT, values.get(index),
                    regular, REVENUE_DATA_FONT_SIZE, index < 2, index == 3);
            cellX += widths[index];
        }
        return y - REVENUE_BODY_ROW_HEIGHT;
    }

    private float drawRevenuePaperTotals(
            PDPageContentStream content,
            float x,
            float y,
            float[] widths,
            BigDecimal total,
            PDFont regular,
            PDFont bold
    ) throws IOException {
        String[] labels = {"Tổng cộng:", "Thuế GTGT:", "Thuế TNCN:"};
        for (int row = 0; row < labels.length; row++) {
            PDFont rowFont = row == 0 ? bold : regular;
            float cellX = x;
            drawPaperCell(content, cellX, y, widths[0], REVENUE_FOOTER_ROW_HEIGHT,
                    "", rowFont, 8.8f, false, false);
            cellX += widths[0];
            drawPaperCell(content, cellX, y, widths[1], REVENUE_FOOTER_ROW_HEIGHT,
                    "", rowFont, 8.8f, false, false);
            cellX += widths[1];
            drawPaperCell(content, cellX, y, widths[2], REVENUE_FOOTER_ROW_HEIGHT,
                    labels[row], rowFont, 8.8f, true, false);
            cellX += widths[2];
            String amount = row == 0 ? formatMoney(total) : "";
            drawPaperCell(content, cellX, y, widths[3], REVENUE_FOOTER_ROW_HEIGHT,
                    amount, rowFont, 8.8f, false, true);
            y -= REVENUE_FOOTER_ROW_HEIGHT;
        }
        return y;
    }

    private void drawPaperCell(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            String value,
            PDFont font,
            float fontSize,
            boolean centered,
            boolean rightAligned
    ) throws IOException {
        content.setStrokingColor(BLACK);
        content.setNonStrokingColor(BLACK);
        content.setLineWidth(REVENUE_GRID_LINE_WIDTH);
        content.addRect(x, y - height, width, height);
        content.stroke();

        List<String> lines = wrapText(display(value), font, fontSize,
                width - CELL_PADDING * 2, height >= 30 ? 2 : 1);
        float leading = fontSize + 2;
        float textY = y - Math.max(CELL_PADDING, (height - lines.size() * leading) / 2) - fontSize;
        for (String line : lines) {
            float textWidth = stringWidth(font, fontSize, line);
            float textX = x + CELL_PADDING;
            if (centered) {
                textX = x + Math.max(CELL_PADDING, (width - textWidth) / 2);
            } else if (rightAligned) {
                textX = x + Math.max(CELL_PADDING, width - CELL_PADDING - textWidth);
            }
            drawText(content, font, fontSize, line, textX, textY);
            textY -= leading;
        }
    }

    private float drawGridRow(
            PDPageContentStream content,
            float x,
            float y,
            float[] widths,
            List<String> values,
            float height,
            PDFont font,
            float fontSize,
            float leading,
            boolean header,
            int rightAlignedColumn
    ) throws IOException {
        float cellX = x;
        for (int index = 0; index < widths.length; index++) {
            drawCell(content, cellX, y, widths[index], height, values.get(index), font,
                    fontSize, leading, header, index == rightAlignedColumn, header ? 8 : 12);
            cellX += widths[index];
        }
        return y - height;
    }

    private void drawCell(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            String value,
            PDFont font,
            float fontSize,
            float leading,
            boolean centered,
            boolean rightAligned,
        int maxLines
    ) throws IOException {
        content.setStrokingColor(BLACK);
        content.setLineWidth(MEDICAL_GRID_LINE_WIDTH);
        content.addRect(x, y - height, width, height);
        content.stroke();
        content.setNonStrokingColor(BLACK);

        List<String> lines = wrapText(display(value), font, fontSize, width - CELL_PADDING * 2, maxLines);
        float textY = y - CELL_PADDING - fontSize;
        for (String line : lines) {
            float textWidth = stringWidth(font, fontSize, line);
            float textX = x + CELL_PADDING;
            if (centered) {
                textX = x + Math.max(CELL_PADDING, (width - textWidth) / 2);
            } else if (rightAligned) {
                textX = x + Math.max(CELL_PADDING, width - CELL_PADDING - textWidth);
            }
            drawText(content, font, fontSize, line, textX, textY);
            textY -= leading;
        }
    }

    private static float rowHeight(
            List<String> values,
            float[] widths,
            PDFont font,
            float fontSize,
            float leading,
            int maxLines
    ) throws IOException {
        int lines = 1;
        for (int index = 0; index < widths.length; index++) {
            lines = Math.max(lines,
                    wrapText(display(values.get(index)), font, fontSize,
                            widths[index] - CELL_PADDING * 2, maxLines).size());
        }
        return Math.max(18, lines * leading + CELL_PADDING * 2);
    }

    private static List<String> wrapText(
            String value, PDFont font, float fontSize, float maxWidth, int maxLines) throws IOException {
        if (value.isBlank()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.replace('\r', '\n').split("\\n+", -1)) {
            String current = "";
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                    current = candidate;
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current);
                    current = "";
                }
                while (!word.isEmpty() && stringWidth(font, fontSize, word) > maxWidth) {
                    int cut = word.length();
                    while (cut > 1 && stringWidth(font, fontSize, word.substring(0, cut)) > maxWidth) {
                        cut--;
                    }
                    lines.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                current = word;
            }
            if (!current.isEmpty()) {
                lines.add(current);
            }
        }
        if (lines.size() <= maxLines) {
            return lines.isEmpty() ? List.of("") : lines;
        }
        List<String> limited = new ArrayList<>(lines.subList(0, maxLines));
        String last = limited.getLast();
        while (!last.isEmpty() && stringWidth(font, fontSize, last + "...") > maxWidth) {
            last = last.substring(0, last.length() - 1);
        }
        limited.set(maxLines - 1, last + "...");
        return limited;
    }

    private List<String> medicalValues(MedicalBookReportRow row) {
        return List.of(
                Integer.toString(row.sequence()), display(row.patientName()), display(row.gender()),
                DatePickerSupport.format(row.birthDate()), display(row.identityDocument()),
                display(row.healthInsuranceNumber()), display(row.address()), display(row.occupation()),
                display(row.ethnicity()), display(row.symptoms()), display(row.diagnosis()),
                display(row.treatment()), display(row.dentist()), display(row.note())
        );
    }

    private List<String> revenueValues(RevenueReportRow row) {
        return List.of(
                display(row.referenceNumber()), DatePickerSupport.format(row.documentDate()),
                display(row.description()), formatMoney(row.amount())
        );
    }

    private static List<List<String>> blankRows(int columns, int count) {
        List<List<String>> rows = new ArrayList<>();
        List<String> blank = java.util.Collections.nCopies(columns, "");
        for (int index = 0; index < count; index++) {
            rows.add(blank);
        }
        return rows;
    }

    private static float[] scaledWidths(float availableWidth, float... proportions) {
        float[] widths = new float[proportions.length];
        for (int index = 0; index < proportions.length; index++) {
            widths[index] = availableWidth * proportions[index];
        }
        return widths;
    }

    private static void drawCenteredText(
            PDPageContentStream content, PDFont font, float size, String text, float pageWidth, float y)
            throws IOException {
        drawText(content, font, size, text, (pageWidth - stringWidth(font, size, text)) / 2, y);
    }

    private static void drawCenteredTextInArea(
            PDPageContentStream content,
            PDFont font,
            float size,
            String text,
            float x,
            float width,
            float y
    ) throws IOException {
        drawText(content, font, size, text,
                x + Math.max(0, (width - stringWidth(font, size, text)) / 2), y);
    }

    private static void drawRightText(
            PDPageContentStream content, PDFont font, float size, String text, float right, float y)
            throws IOException {
        drawText(content, font, size, text, right - stringWidth(font, size, text), y);
    }

    private static void drawText(
            PDPageContentStream content, PDFont font, float size, String text, float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(display(text));
        content.endText();
    }

    private static float stringWidth(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(display(text)) / 1000f * size;
    }

    private static String formatMoney(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static String display(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u0000', ' ').replace('\t', ' ').trim();
    }

    private static String paperField(String label, String value, int dotCount) {
        String normalized = display(value);
        boolean placeholder = normalized.isBlank()
                || normalized.equalsIgnoreCase("Chưa cấu hình")
                || normalized.equalsIgnoreCase("PHÒNG KHÁM NHA KHOA");
        return label + " " + (placeholder ? "" : normalized + " ") + ".".repeat(dotCount);
    }

    private static String dotsMatchingWidth(PDFont font, float size, float targetWidth) throws IOException {
        StringBuilder dots = new StringBuilder(".");
        while (stringWidth(font, size, dots + ".") <= targetWidth) {
            dots.append('.');
        }
        return dots.toString();
    }

    private static void createParentDirectory(Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static PDFont loadFont(PDDocument document, boolean bold) throws IOException {
        String resource = bold ? "/fonts/NotoSans-Bold.ttf" : "/fonts/NotoSans-Regular.ttf";
        try (InputStream stream = PdfReportService.class.getResourceAsStream(resource)) {
            if (stream != null) {
                return PDType0Font.load(document, stream);
            }
        }

        Path fontPath = findSystemFont(bold);
        try (InputStream stream = Files.newInputStream(fontPath)) {
            return PDType0Font.load(document, stream);
        }
    }

    private static PDFont loadItalicFont(PDDocument document) throws IOException {
        Path fontPath = findSystemItalicFont();
        try (InputStream stream = Files.newInputStream(fontPath)) {
            return PDType0Font.load(document, stream);
        }
    }

    private static Path findSystemFont(boolean bold) throws IOException {
        String configured = System.getenv(bold ? "DENTAL_PDF_BOLD_FONT" : "DENTAL_PDF_FONT");
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured));
        }
        if (bold) {
            candidates.addAll(List.of(
                    Path.of("C:/Windows/Fonts/timesbd.ttf"),
                    Path.of("C:/Windows/Fonts/arialbd.ttf"),
                    Path.of("C:/Windows/Fonts/segoeuib.ttf"),
                    Path.of("/usr/share/fonts/truetype/liberation2/LiberationSerif-Bold.ttf"),
                    Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial Bold.ttf")
            ));
        } else {
            candidates.addAll(List.of(
                    Path.of("C:/Windows/Fonts/times.ttf"),
                    Path.of("C:/Windows/Fonts/arial.ttf"),
                    Path.of("C:/Windows/Fonts/segoeui.ttf"),
                    Path.of("/usr/share/fonts/truetype/liberation2/LiberationSerif-Regular.ttf"),
                    Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial.ttf")
            ));
        }
        return candidates.stream().filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new IOException(
                        "Không tìm thấy font Unicode. Hãy cấu hình DENTAL_PDF_FONT và DENTAL_PDF_BOLD_FONT."));
    }

    private static Path findSystemItalicFont() throws IOException {
        String configured = System.getenv("DENTAL_PDF_ITALIC_FONT");
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured));
        }
        candidates.addAll(List.of(
                Path.of("C:/Windows/Fonts/timesi.ttf"),
                Path.of("C:/Windows/Fonts/ariali.ttf"),
                Path.of("/usr/share/fonts/truetype/liberation2/LiberationSerif-Italic.ttf"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Italic.ttf"),
                Path.of("/System/Library/Fonts/Supplemental/Times New Roman Italic.ttf")
        ));
        return candidates.stream().filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new IOException(
                        "Không tìm thấy font nghiêng Unicode. Hãy cấu hình DENTAL_PDF_ITALIC_FONT."));
    }
}
