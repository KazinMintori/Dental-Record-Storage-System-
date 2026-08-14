package com.dentalclinic.controller;

import com.dentalclinic.model.report.ClinicInfo;
import com.dentalclinic.model.report.MedicalBookReportRow;
import com.dentalclinic.model.report.RevenueReportRow;
import com.dentalclinic.repository.ReportRepository;
import com.dentalclinic.service.PdfReportService;
import com.dentalclinic.service.ReportService;
import com.dentalclinic.util.DatePickerSupport;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReportController {

    private static final int REVENUE_PREVIEW_ROWS_PER_PAGE = 14;

    private final ReportService reportService;
    private final PdfReportService pdfReportService;
    private final ClinicInfo clinicInfo;
    private final Executor executor;

    @FXML private ToggleButton patientBookOption;
    @FXML private ToggleButton revenueOption;
    @FXML private Label reportDescription;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button previewButton;
    @FXML private Button printButton;
    @FXML private ProgressIndicator reportProgress;
    @FXML private Label reportStatus;

    public ReportController() {
        this(new ReportService(new ReportRepository()), new PdfReportService(),
                ClinicInfo.fromEnvironment(), ReportController::runInBackground);
    }

    public ReportController(ReportService reportService, PdfReportService pdfReportService) {
        this(reportService, pdfReportService, ClinicInfo.fromEnvironment(), ReportController::runInBackground);
    }

    ReportController(
            ReportService reportService,
            PdfReportService pdfReportService,
            ClinicInfo clinicInfo,
            Executor executor
    ) {
        this.reportService = Objects.requireNonNull(reportService, "reportService must not be null");
        this.pdfReportService = Objects.requireNonNull(pdfReportService, "pdfReportService must not be null");
        this.clinicInfo = Objects.requireNonNull(clinicInfo, "clinicInfo must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    @FXML
    private void initialize() {
        DatePickerSupport.configure(fromDatePicker);
        DatePickerSupport.configure(toDatePicker);
        patientBookOption.setSelected(true);
        selectPatientBook();
    }

    @FXML
    public void selectPatientBook() {
        patientBookOption.setSelected(true);
        revenueOption.setSelected(false);
        reportDescription.setText(
                "Các lần khám được tạo trên hệ thống trong khoảng ngày đã chọn (PDF khổ ngang)."
        );
    }

    @FXML
    public void selectRevenue() {
        patientBookOption.setSelected(false);
        revenueOption.setSelected(true);
        reportDescription.setText(
                "Doanh thu thuộc các lần khám được tạo trên hệ thống trong khoảng ngày đã chọn (PDF khổ dọc)."
        );
    }

    @FXML
    public void previewReport() {
        DateRange range = validatedRange();
        if (range == null) {
            return;
        }
        ReportType reportType = selectedReportType();
        executeTask(reportTask(range, reportType), this::showPreview, "Không thể tải dữ liệu xem trước.");
    }

    @FXML
    public void exportPdf() {
        DateRange range = validatedRange();
        if (range == null) {
            return;
        }
        ReportType reportType = selectedReportType();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn nơi lưu báo cáo PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp PDF", "*.pdf"));
        chooser.setInitialFileName(defaultFileName(range, reportType));
        File selected = chooser.showSaveDialog(ownerWindow());
        if (selected == null) {
            return;
        }
        Path output = ensurePdfExtension(selected.toPath());
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                LoadedReport report = loadReport(range, reportType);
                if (report.type() == ReportType.MEDICAL_BOOK) {
                    pdfReportService.exportMedicalBook(
                            output, report.medicalRows(), range.from(), range.to());
                } else {
                    pdfReportService.exportRevenueReport(
                            output, report.revenueRows(), clinicInfo, range.from(), range.to());
                }
                return output;
            }
        };
        executeTask(task, path -> {
            reportStatus.setText("Đã xuất PDF: " + path.getFileName());
            showAlert(Alert.AlertType.INFORMATION, "Xuất PDF thành công", path.toAbsolutePath().toString());
        }, "Không thể xuất tệp PDF.");
    }

    private Task<LoadedReport> reportTask(DateRange range, ReportType reportType) {
        return new Task<>() {
            @Override
            protected LoadedReport call() {
                return loadReport(range, reportType);
            }
        };
    }

    private LoadedReport loadReport(DateRange range, ReportType reportType) {
        if (reportType == ReportType.MEDICAL_BOOK) {
            return LoadedReport.medical(reportService.getMedicalBook(range.from(), range.to()), range);
        }
        return LoadedReport.revenue(reportService.getRevenueReport(range.from(), range.to()), range);
    }

    private <T> void executeTask(Task<T> task, Consumer<T> onSuccess, String errorMessage) {
        setLoading(true);
        task.setOnSucceeded(event -> {
            setLoading(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            setLoading(false);
            reportStatus.setText(errorMessage);
            showAlert(Alert.AlertType.ERROR, "Không thể tạo báo cáo", errorMessage);
        });
        executor.execute(task);
    }

    private void showPreview(LoadedReport report) {
        Stage stage = new Stage();
        stage.setTitle(report.type() == ReportType.MEDICAL_BOOK
                ? "Xem trước - Sổ khám bệnh" : "Xem trước - Sổ doanh thu");
        stage.initModality(Modality.WINDOW_MODAL);
        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("report-preview-root");

        if (report.type() == ReportType.MEDICAL_BOOK) {
            root.getStyleClass().add("medical-report-preview-root");
            Label title = new Label("SỔ KHÁM BỆNH");
            title.getStyleClass().add("report-preview-title");
            Label period = new Label("Từ ngày " + DatePickerSupport.format(report.range().from())
                    + " đến ngày " + DatePickerSupport.format(report.range().to()));
            period.getStyleClass().add("helper-text");
            root.setTop(new VBox(5, title, period));
            root.setCenter(medicalPreviewTable(report.medicalRows()));
        } else {
            root.getStyleClass().add("revenue-paper-preview-root");
            root.setCenter(revenuePaperPreview(report.revenueRows()));
        }

        Scene scene = new Scene(root, 1200, 780);
        if (ReportController.class.getResource("/css/style.css") != null) {
            scene.getStylesheets().add(
                    Objects.requireNonNull(ReportController.class.getResource("/css/style.css")).toExternalForm());
        }
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.show();
    }

    private Node medicalPreviewTable(List<MedicalBookReportRow> rows) {
        var items = FXCollections.observableArrayList(rows);
        TableView<MedicalBookReportRow> frozenTable = previewTable();
        frozenTable.getStyleClass().add("report-preview-frozen-table");
        frozenTable.getColumns().addAll(
                column("TT", 60, row -> Integer.toString(row.sequence())),
                column("Họ và tên", 190, MedicalBookReportRow::patientName)
        );
        frozenTable.setItems(items);
        frozenTable.setMinWidth(265);
        frozenTable.setPrefWidth(265);
        frozenTable.setMaxWidth(265);

        TableView<MedicalBookReportRow> scrollingTable = previewTable();
        scrollingTable.getStyleClass().add("report-preview-scrolling-table");
        scrollingTable.getColumns().addAll(
                column("Giới tính", 90, MedicalBookReportRow::gender),
                column("Ngày tháng năm (sinh)", 150, row -> DatePickerSupport.format(row.birthDate())),
                column("ĐDCN/Giấy tờ tuỳ thân", 180, MedicalBookReportRow::identityDocument),
                column("Số thẻ BHYT", 140, MedicalBookReportRow::healthInsuranceNumber),
                column("Địa chỉ", 220, MedicalBookReportRow::address),
                column("Nghề nghiệp", 140, MedicalBookReportRow::occupation),
                column("Dân tộc", 100, MedicalBookReportRow::ethnicity),
                column("Triệu chứng", 220, MedicalBookReportRow::symptoms),
                column("Chẩn đoán", 220, MedicalBookReportRow::diagnosis),
                column("Phương pháp điều trị", 240, MedicalBookReportRow::treatment),
                column("Y,BS khám bệnh", 160, MedicalBookReportRow::dentist),
                column("Ghi chú", 200, MedicalBookReportRow::note)
        );
        scrollingTable.setItems(items);
        synchronizeSelection(frozenTable, scrollingTable);

        HBox container = new HBox(frozenTable, scrollingTable);
        container.getStyleClass().add("report-preview-frozen-container");
        HBox.setHgrow(scrollingTable, Priority.ALWAYS);
        Platform.runLater(() -> synchronizeVerticalScroll(frozenTable, scrollingTable));
        return container;
    }

    private Node revenuePaperPreview(List<RevenueReportRow> rows) {
        int pageCount = Math.max(1,
                (rows.size() + REVENUE_PREVIEW_ROWS_PER_PAGE - 1) / REVENUE_PREVIEW_ROWS_PER_PAGE);
        BigDecimal periodTotal = ReportService.totalRevenue(rows);
        VBox pages = new VBox(22);
        pages.setAlignment(Pos.TOP_CENTER);
        pages.getStyleClass().add("revenue-paper-pages");

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int fromIndex = pageIndex * REVENUE_PREVIEW_ROWS_PER_PAGE;
            int toIndex = Math.min(rows.size(), fromIndex + REVENUE_PREVIEW_ROWS_PER_PAGE);
            List<RevenueReportRow> pageRows = fromIndex < toIndex
                    ? rows.subList(fromIndex, toIndex) : List.of();
            pages.getChildren().add(revenuePaperPage(pageRows, periodTotal));
        }

        ScrollPane scroll = new ScrollPane(pages);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("revenue-paper-scroll");
        return scroll;
    }

    private Node revenuePaperPage(List<RevenueReportRow> rows, BigDecimal periodTotal) {
        VBox page = new VBox();
        page.setMinWidth(800);
        page.setPrefWidth(800);
        page.setMaxWidth(800);
        page.getStyleClass().add("revenue-paper-page");

        VBox business = new VBox(3,
                paperLabel("HỘ, CÁ NHÂN KINH DOANH: ......", "revenue-paper-meta-bold"),
                paperLabel("..............................................................", "revenue-paper-meta-bold"),
                paperLabel("Mã số thuế:........................................", "revenue-paper-meta-bold"),
                paperLabel("Địa chỉ:................................................", "revenue-paper-meta-bold"));
        business.setMinWidth(425);

        VBox formNumber = new VBox(1,
                paperLabel("Mẫu số S2a-HKD", "revenue-paper-meta-bold"),
                paperLabel("(Kèm theo Thông tư số 152/2025/TT-BTC", "revenue-paper-meta-italic"),
                paperLabel("ngày 31 tháng 12 năm 2025 của Bộ trưởng", "revenue-paper-meta-italic"),
                paperLabel("Bộ Tài chính)", "revenue-paper-meta-italic"));
        formNumber.setAlignment(Pos.TOP_CENTER);
        HBox metadata = new HBox(business, formNumber);
        HBox.setHgrow(formNumber, Priority.ALWAYS);

        Label title = paperLabel("SỔ DOANH THU BÁN HÀNG HÓA, DỊCH VỤ", "revenue-paper-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        Label location = paperLabel(
                "Địa điểm kinh doanh:.................................", "revenue-paper-field");
        Label period = paperLabel(
                "Kỳ kê khai:................................................", "revenue-paper-field");
        location.setMaxWidth(Double.MAX_VALUE);
        period.setMaxWidth(Double.MAX_VALUE);
        location.setAlignment(Pos.CENTER);
        period.setAlignment(Pos.CENTER);
        VBox heading = new VBox(5, title, location, period);
        VBox.setMargin(heading, new Insets(28, 0, 24, 0));

        GridPane table = revenuePaperGrid(rows, periodTotal);
        VBox.setMargin(table, new Insets(0, 0, 22, 0));

        VBox signature = new VBox(4,
                paperLabel("Ngày ... tháng ... năm ...", "revenue-paper-signature-italic"),
                paperLabel("NGƯỜI ĐẠI DIỆN HỘ KINH DOANH", "revenue-paper-signature-bold"),
                paperLabel("(Ký, họ tên, đóng dấu)", "revenue-paper-signature-italic"));
        signature.setAlignment(Pos.CENTER);
        signature.setMinWidth(325);
        HBox signatureRow = new HBox(new Region(), signature);
        HBox.setHgrow(signatureRow.getChildren().getFirst(), Priority.ALWAYS);
        VBox.setMargin(signatureRow, new Insets(0, 36, 0, 0));

        page.getChildren().addAll(metadata, heading, table, signatureRow);
        return page;
    }

    private GridPane revenuePaperGrid(List<RevenueReportRow> rows, BigDecimal periodTotal) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("revenue-paper-grid");
        double[] percentages = {18, 20, 42, 20};
        for (double percentage : percentages) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percentage);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }

        addPaperCell(grid, paperCell("Chứng từ", Pos.CENTER, "revenue-paper-header-cell"), 0, 0, 2, 1);
        addPaperCell(grid, paperCell("Diễn giải", Pos.CENTER, "revenue-paper-header-cell"), 2, 0, 1, 2);
        addPaperCell(grid, paperCell("Số tiền", Pos.CENTER, "revenue-paper-header-cell"), 3, 0, 1, 2);
        addPaperCell(grid, paperCell("Số hiệu", Pos.CENTER, "revenue-paper-header-cell"), 0, 1, 1, 1);
        addPaperCell(grid, paperCell("Ngày, tháng", Pos.CENTER, "revenue-paper-header-cell"), 1, 1, 1, 1);

        String[] codes = {"A", "B", "C", "1"};
        for (int column = 0; column < codes.length; column++) {
            addPaperCell(grid, paperCell(codes[column], Pos.CENTER, "revenue-paper-code-cell"),
                    column, 2, 1, 1);
        }

        for (int rowIndex = 0; rowIndex < REVENUE_PREVIEW_ROWS_PER_PAGE; rowIndex++) {
            RevenueReportRow row = rowIndex < rows.size() ? rows.get(rowIndex) : null;
            String[] values = row == null
                    ? new String[] {"", "", "", ""}
                    : new String[] {
                            display(row.referenceNumber()),
                            DatePickerSupport.format(row.documentDate()),
                            display(row.description()),
                            formatMoney(row.amount())
                    };
            int gridRow = rowIndex + 3;
            addPaperCell(grid, paperCell(values[0], Pos.CENTER, "revenue-paper-data-cell"),
                    0, gridRow, 1, 1);
            addPaperCell(grid, paperCell(values[1], Pos.CENTER, "revenue-paper-data-cell"),
                    1, gridRow, 1, 1);
            addPaperCell(grid, paperCell(values[2], Pos.CENTER_LEFT, "revenue-paper-data-cell"),
                    2, gridRow, 1, 1);
            addPaperCell(grid, paperCell(values[3], Pos.CENTER_RIGHT, "revenue-paper-data-cell"),
                    3, gridRow, 1, 1);
        }

        String[] labels = {"Tổng cộng:", "Thuế GTGT:", "Thuế TNCN:"};
        for (int row = 0; row < labels.length; row++) {
            int gridRow = 3 + REVENUE_PREVIEW_ROWS_PER_PAGE + row;
            String style = row == 0 ? "revenue-paper-total-cell" : "revenue-paper-footer-cell";
            addPaperCell(grid, paperCell("", Pos.CENTER, style), 0, gridRow, 1, 1);
            addPaperCell(grid, paperCell("", Pos.CENTER, style), 1, gridRow, 1, 1);
            addPaperCell(grid, paperCell(labels[row], Pos.CENTER, style), 2, gridRow, 1, 1);
            addPaperCell(grid, paperCell(row == 0 ? formatMoney(periodTotal) : "", Pos.CENTER_RIGHT, style),
                    3, gridRow, 1, 1);
        }
        return grid;
    }

    private static Label paperLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static Label paperCell(String text, Pos alignment, String styleClass) {
        Label label = paperLabel(text, styleClass);
        label.getStyleClass().add("revenue-paper-cell");
        label.setAlignment(alignment);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setMinHeight(styleClass.equals("revenue-paper-code-cell") ? 26 : 34);
        return label;
    }

    private static void addPaperCell(
            GridPane grid, Label cell, int column, int row, int columnSpan, int rowSpan) {
        grid.add(cell, column, row, columnSpan, rowSpan);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        GridPane.setVgrow(cell, Priority.ALWAYS);
    }

    private static <T> TableView<T> previewTable() {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("report-preview-table");
        table.setPlaceholder(new Label(""));
        return table;
    }

    private static <T> void synchronizeSelection(TableView<T> frozen, TableView<T> scrolling) {
        frozen.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            if (index >= 0 && scrolling.getSelectionModel().getSelectedIndex() != index) {
                scrolling.getSelectionModel().select(index);
                scrolling.scrollTo(index);
            }
        });
        scrolling.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            if (index >= 0 && frozen.getSelectionModel().getSelectedIndex() != index) {
                frozen.getSelectionModel().select(index);
                frozen.scrollTo(index);
            }
        });
    }

    private static void synchronizeVerticalScroll(TableView<?> frozen, TableView<?> scrolling) {
        ScrollBar frozenBar = verticalScrollBar(frozen);
        ScrollBar scrollingBar = verticalScrollBar(scrolling);
        if (frozenBar == null || scrollingBar == null) {
            return;
        }
        frozenBar.valueProperty().bindBidirectional(scrollingBar.valueProperty());
        frozenBar.setOpacity(0);
        frozenBar.setMouseTransparent(true);
    }

    private static ScrollBar verticalScrollBar(TableView<?> table) {
        return table.lookupAll(".scroll-bar").stream()
                .filter(ScrollBar.class::isInstance)
                .map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                .findFirst()
                .orElse(null);
    }

    private static <T> TableColumn<T, String> column(
            String title, double width, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(display(value.apply(cell.getValue()))));
        return column;
    }

    private DateRange validatedRange() {
        try {
            LocalDate from = committedValue(fromDatePicker);
            LocalDate to = committedValue(toDatePicker);
            ReportService.validateDates(from, to);
            return new DateRange(from, to);
        } catch (DateTimeParseException exception) {
            showAlert(Alert.AlertType.ERROR, "Ngày tháng không hợp lệ",
                    "Vui lòng nhập ngày theo định dạng Ngày/Tháng/Năm.");
        } catch (IllegalArgumentException exception) {
            String message = exception.getMessage();
            showAlert(Alert.AlertType.ERROR, message, message);
        }
        return null;
    }

    private static LocalDate committedValue(DatePicker picker) {
        return DatePickerSupport.commit(picker);
    }

    private ReportType selectedReportType() {
        return revenueOption.isSelected() ? ReportType.REVENUE : ReportType.MEDICAL_BOOK;
    }

    private String defaultFileName(DateRange range, ReportType reportType) {
        String prefix = reportType == ReportType.MEDICAL_BOOK
                ? "so-kham-benh" : "so-doanh-thu";
        return prefix + "-" + range.from() + "-den-" + range.to() + ".pdf";
    }

    private void setLoading(boolean loading) {
        previewButton.setDisable(loading);
        printButton.setDisable(loading);
        reportProgress.setVisible(loading);
        reportProgress.setManaged(loading);
        reportStatus.setText(loading ? "Đang truy vấn dữ liệu theo thời gian tạo lần khám..." : "");
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("In báo cáo");
        alert.setHeaderText(header);
        alert.setContentText(content);
        Window owner = ownerWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private Window ownerWindow() {
        return previewButton == null || previewButton.getScene() == null
                ? null : previewButton.getScene().getWindow();
    }

    private static Path ensurePdfExtension(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? path : path.resolveSibling(path.getFileName() + ".pdf");
    }

    private static String formatMoney(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static String display(String value) {
        return value == null ? "" : value;
    }

    private static void runInBackground(Runnable command) {
        Thread thread = new Thread(command, "dental-report");
        thread.setDaemon(true);
        thread.start();
    }

    private enum ReportType {
        MEDICAL_BOOK,
        REVENUE
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private record LoadedReport(
            ReportType type,
            List<MedicalBookReportRow> medicalRows,
            List<RevenueReportRow> revenueRows,
            DateRange range
    ) {
        private static LoadedReport medical(List<MedicalBookReportRow> rows, DateRange range) {
            return new LoadedReport(ReportType.MEDICAL_BOOK, List.copyOf(rows), List.of(), range);
        }

        private static LoadedReport revenue(List<RevenueReportRow> rows, DateRange range) {
            return new LoadedReport(ReportType.REVENUE, List.of(), List.copyOf(rows), range);
        }
    }
}
