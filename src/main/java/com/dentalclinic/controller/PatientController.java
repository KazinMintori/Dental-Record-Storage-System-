package com.dentalclinic.controller;

import com.dentalclinic.model.Patient;
import com.dentalclinic.model.Revenue;
import com.dentalclinic.model.Visit;
import com.dentalclinic.service.PatientService;
import com.dentalclinic.service.RevenueService;
import com.dentalclinic.service.ServiceException;
import com.dentalclinic.service.VisitService;
import com.dentalclinic.service.VisitRevenueWorkflowService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PatientController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String EMPTY_VALUE = "—";

    private final PatientService patientService;
    private final VisitService visitService;
    private final RevenueService revenueService;
    private final VisitRevenueWorkflowService workflowService;
    private final Executor executor;

    @FXML private TextField searchField;
    @FXML private TilePane patientListContainer;
    @FXML private Label listMessage;
    @FXML private VBox directoryEmptyState;
    @FXML private StackPane directoryContent;
    @FXML private Label statusMessage;
    @FXML private VBox directoryView;
    @FXML private VBox detailView;
    @FXML private VBox patientFormView;
    @FXML private Label selectedPatientName;
    @FXML private Label patientNameValue;
    @FXML private Label patientGenderValue;
    @FXML private Label patientBirthDateValue;
    @FXML private Label patientIdentityValue;
    @FXML private Label patientInsuranceValue;
    @FXML private Label patientAddressValue;
    @FXML private Label patientOccupationValue;
    @FXML private Label patientEthnicityValue;
    @FXML private Button editPatientButton;
    @FXML private Button addVisitButton;
    @FXML private TitledPane patientFormPane;
    @FXML private Label patientFormTitle;
    @FXML private TextField patientNameField;
    @FXML private TextField patientGenderField;
    @FXML private DatePicker patientBirthDateField;
    @FXML private TextField patientIdentityField;
    @FXML private TextField patientInsuranceField;
    @FXML private TextField patientAddressField;
    @FXML private TextField patientOccupationField;
    @FXML private TextField patientEthnicityField;
    @FXML private TitledPane visitFormPane;
    @FXML private DatePicker visitDateField;
    @FXML private TextField dentistField;
    @FXML private TextArea symptomsField;
    @FXML private TextArea diagnosisField;
    @FXML private TextArea treatmentField;
    @FXML private TextArea visitNoteField;
    @FXML private VBox visitListContainer;
    @FXML private Label visitCountLabel;
    @FXML private VBox revenueRowsContainer;
    @FXML private Label revenueTotalLabel;
    @FXML private Label visitFormHeading;
    @FXML private Button deleteVisitButton;

    private Patient selectedPatient;
    private List<Visit> selectedPatientVisits = List.of();
    private boolean creatingPatient;
    private Visit editingVisit;
    private final List<RevenueRow> revenueRows = new ArrayList<>();

    public PatientController(
            PatientService patientService,
            VisitService visitService,
            RevenueService revenueService,
            VisitRevenueWorkflowService workflowService
    ) {
        this(patientService, visitService, revenueService, workflowService, PatientController::runInBackground);
    }

    public PatientController(
            PatientService patientService,
            VisitService visitService,
            RevenueService revenueService,
            VisitRevenueWorkflowService workflowService,
            Executor executor
    ) {
        this.patientService = Objects.requireNonNull(patientService, "patientService must not be null");
        this.visitService = Objects.requireNonNull(visitService, "visitService must not be null");
        this.revenueService = Objects.requireNonNull(revenueService, "revenueService must not be null");
        this.workflowService = Objects.requireNonNull(workflowService, "workflowService must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    private static void runInBackground(Runnable command) {
        Thread thread = new Thread(command, "dental-ui-data");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void initialize() {
        showDirectory();
        refreshPatients();
    }

    @FXML
    public void refreshPatients() {
        loadPatients("");
    }

    @FXML
    public void searchPatients() {
        loadPatients(searchField.getText());
    }

    @FXML
    public void openNewPatientForm() {
        creatingPatient = true;
        clearPatientForm();
        patientFormTitle.setText("Thêm bệnh nhân");
        patientFormPane.setText("Thêm bệnh nhân");
        patientFormPane.setExpanded(true);
        showOnly(patientFormView);
        clearStatus();
    }

    @FXML
    public void openEditPatientForm() {
        if (selectedPatient == null) {
            showError("Vui lòng chọn bệnh nhân cần chỉnh sửa.");
            return;
        }
        creatingPatient = false;
        fillPatientForm(selectedPatient);
        patientFormTitle.setText("Chỉnh sửa thông tin bệnh nhân");
        patientFormPane.setText("Chỉnh sửa thông tin bệnh nhân");
        patientFormPane.setExpanded(true);
        showOnly(patientFormView);
        clearStatus();
    }

    @FXML
    public void savePatient() {
        if (patientBirthDateField.getValue() == null) {
            showError("Vui lòng nhập đầy đủ thông tin bắt buộc.");
            return;
        }

        Patient patient = patientFromForm();
        if (creatingPatient) {
            runOperation(
                    () -> patientService.createPatient(patient),
                    saved -> finishPatientSave(saved, "Đã thêm bệnh nhân thành công."),
                    "Không thể lưu thông tin bệnh nhân."
            );
            return;
        }
        if (selectedPatient == null) {
            showError("Vui lòng chọn bệnh nhân cần chỉnh sửa.");
            return;
        }

        copyIdentity(selectedPatient, patient);
        runOperation(
                () -> {
                    patientService.updatePatient(patient);
                    Patient refreshed = patientService.getPatient(patient.getId());
                    return refreshed == null ? patient : refreshed;
                },
                saved -> finishPatientSave(saved, "Đã cập nhật thông tin bệnh nhân."),
                "Không thể lưu thông tin bệnh nhân."
        );
    }

    @FXML
    public void cancelPatientForm() {
        patientFormPane.setExpanded(false);
        clearPatientForm();
        clearStatus();
        if (selectedPatient == null || creatingPatient) {
            showDirectory();
        } else {
            showOnly(detailView);
        }
    }

    @FXML
    public void showDirectory() {
        showOnly(directoryView);
        clearStatus();
    }

    @FXML
    public void openVisitForm() {
        if (selectedPatient == null) {
            showError("Vui lòng chọn bệnh nhân trước khi thêm lần khám.");
            return;
        }
        clearVisitForm();
        editingVisit = null;
        visitFormHeading.setText("Thêm lần khám");
        deleteVisitButton.setVisible(false);
        deleteVisitButton.setManaged(false);
        visitDateField.setValue(LocalDate.now());
        visitFormPane.setExpanded(true);
        clearStatus();
    }

    @FXML
    public void saveVisit() {
        if (selectedPatient == null) {
            showError("Vui lòng chọn bệnh nhân trước khi thêm lần khám.");
            return;
        }
        if (visitDateField.getValue() == null) {
            showError("Vui lòng nhập đầy đủ thông tin bắt buộc.");
            return;
        }

        Visit visit = new Visit(
                selectedPatient.getId(),
                editingVisit == null ? nextVisitSequence() : editingVisit.getTt(),
                visitDateField.getValue(),
                symptomsField.getText(),
                diagnosisField.getText(),
                treatmentField.getText(),
                dentistField.getText()
        );
        visit.setGhiChu(emptyToNull(visitNoteField.getText()));
        if (editingVisit != null) {
            visit.setId(editingVisit.getId());
            visit.setCreatedAt(editingVisit.getCreatedAt());
            visit.setUpdatedAt(editingVisit.getUpdatedAt());
        }
        List<Revenue> revenues;
        try {
            revenues = revenueRows.stream().map(RevenueRow::toRevenue).toList();
        } catch (IllegalArgumentException exception) {
            showError("Vui lòng nhập đầy đủ thông tin doanh thu hợp lệ.");
            return;
        }
        Long patientId = selectedPatient.getId();
        runOperation(
                () -> {
                    if (editingVisit == null) {
                        workflowService.create(visit, revenues);
                    } else {
                        workflowService.update(visit, revenues);
                    }
                    return visitService.getPatientVisits(patientId);
                },
                visits -> {
                    selectedPatientVisits = List.copyOf(visits);
                    renderVisits(visits);
                    clearVisitForm();
                    visitFormPane.setExpanded(false);
                    editingVisit = null;
                    showSuccess("Đã lưu lần khám thành công.");
                },
                "Không thể lưu lần khám."
        );
    }

    @FXML
    public void cancelVisitForm() {
        visitFormPane.setExpanded(false);
        clearVisitForm();
        clearStatus();
    }

    @FXML
    public void addRevenueRow() {
        Revenue revenue = new Revenue(0L,
                visitDateField.getValue() == null ? LocalDate.now() : visitDateField.getValue(),
                treatmentField.getText(), BigDecimal.ZERO);
        addRevenueRow(revenue);
    }

    @FXML
    public void deleteCurrentVisit() {
        if (editingVisit == null) {
            return;
        }
        javafx.scene.control.Alert confirmation = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa lần khám này?",
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK
        );
        confirmation.setHeaderText("Xóa lần khám");
        if (confirmation.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) {
            return;
        }
        Long patientId = selectedPatient.getId();
        Long visitId = editingVisit.getId();
        runOperation(() -> {
            workflowService.delete(visitId);
            return visitService.getPatientVisits(patientId);
        }, visits -> {
            selectedPatientVisits = List.copyOf(visits);
            renderVisits(visits);
            cancelVisitForm();
            showSuccess("Đã xóa lần khám.");
        }, "Không thể xóa lần khám.");
    }

    public Patient getSelectedPatient() {
        return selectedPatient;
    }

    public int getDisplayedPatientCount() {
        return patientListContainer.getChildren().size();
    }

    public int getDisplayedVisitCount() {
        return visitListContainer.getChildren().size();
    }

    public int getRevenueRowCount() {
        return revenueRows.size();
    }

    public BigDecimal getRevenueTotal() {
        return revenueRows.stream()
                .map(RevenueRow::amountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void loadPatients(String searchText) {
        listMessage.setText("Đang tải danh sách bệnh nhân...");
        Supplier<List<Patient>> request = searchText == null || searchText.isBlank()
                ? patientService::getAllPatients
                : () -> patientService.searchPatients(searchText);
        runOperation(
                request,
                this::renderPatients,
                "Không thể tải danh sách bệnh nhân."
        );
    }

    private void renderPatients(List<Patient> patients) {
        patientListContainer.getChildren().clear();
        for (Patient patient : patients) {
            patientListContainer.getChildren().add(createPatientCard(patient));
        }
        listMessage.setText(patients.isEmpty() ? "Chưa có bệnh nhân\nThêm bệnh nhân đầu tiên để bắt đầu" : "");
        directoryEmptyState.setVisible(patients.isEmpty());
        directoryEmptyState.setManaged(patients.isEmpty());
    }

    private VBox createPatientCard(Patient patient) {
        Label name = new Label(patient.getHoVaTen());
        name.getStyleClass().add("patient-name");
        Label birthDate = new Label("Ngày sinh: " + formatDate(patient.getNgaySinh()));
        birthDate.getStyleClass().add("patient-birth-date");
        VBox card = new VBox(5, name, birthDate);
        card.getStyleClass().add("patient-card");
        card.setUserData(patient);
        card.setPrefWidth(210);
        card.setMinHeight(92);
        if (Objects.equals(patient.getId(), selectedPatient == null ? null : selectedPatient.getId())) {
            card.getStyleClass().add("patient-card-selected");
        }
        card.setOnMouseClicked(event -> selectPatient(patient));
        return card;
    }

    private void selectPatient(Patient patient) {
        selectedPatient = patient;
        showPatient(patient);
        showOnly(detailView);
        updateSelectedCardState();
        if (patient == null) {
            selectedPatientVisits = List.of();
            renderVisits(List.of());
            return;
        }
        runOperation(
                () -> visitService.getPatientVisits(patient.getId()),
                visits -> {
                    if (selectedPatient != null && Objects.equals(selectedPatient.getId(), patient.getId())) {
                        selectedPatientVisits = List.copyOf(visits);
                        renderVisits(visits);
                    }
                },
                "Không thể tải lịch sử khám."
        );
    }

    private void showPatient(Patient patient) {
        boolean hasPatient = patient != null;
        editPatientButton.setDisable(!hasPatient);
        addVisitButton.setDisable(!hasPatient);
        if (!hasPatient) {
            return;
        }
        selectedPatientName.setText(patient.getHoVaTen());
        patientNameValue.setText(patient.getHoVaTen());
        patientGenderValue.setText(patient.getGioiTinh());
        patientBirthDateValue.setText(formatDate(patient.getNgaySinh()));
        patientIdentityValue.setText(displayOptional(patient.getGiayToTuyThan()));
        patientInsuranceValue.setText(displayOptional(patient.getSoTheBhyt()));
        patientAddressValue.setText(displayOptional(patient.getDiaChi()));
        patientOccupationValue.setText(displayOptional(patient.getNgheNghiep()));
        patientEthnicityValue.setText(displayOptional(patient.getDanToc()));
    }

    private void renderVisits(List<Visit> visits) {
        visitListContainer.getChildren().clear();
        visitCountLabel.setText(visits.size() + " lần khám");
        if (visits.isEmpty()) {
            Label empty = new Label(selectedPatient == null
                    ? "Chọn bệnh nhân để xem lịch sử khám."
                    : "Bệnh nhân chưa có lần khám nào.");
            empty.getStyleClass().add("helper-text");
            visitListContainer.getChildren().add(empty);
            return;
        }
        for (Visit visit : visits) {
            visitListContainer.getChildren().add(createVisitCard(visit));
        }
    }

    private VBox createVisitCard(Visit visit) {
        Label date = new Label(formatDate(visit.getNgayKham()));
        date.getStyleClass().add("visit-date");
        GridPane details = new GridPane();
        details.setHgap(22);
        details.setVgap(10);
        details.add(fieldBlock("Triệu chứng", visit.getTrieuChung()), 0, 0);
        details.add(fieldBlock("Chẩn đoán", visit.getChanDoan()), 1, 0);
        details.add(fieldBlock("Phương pháp điều trị", visit.getPhuongPhapDieuTri()), 0, 1);
        details.add(fieldBlock("Bác sĩ khám", visit.getBacSiKham()), 1, 1);
        Label note = new Label("Ghi chú: " + displayOptional(visit.getGhiChu()));
        note.getStyleClass().add("muted-text");
        note.setWrapText(true);
        VBox card = new VBox(10, date, details, note);
        card.getStyleClass().add("visit-card");
        card.setOnMouseClicked(event -> openExistingVisit(visit));
        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    private void openExistingVisit(Visit visit) {
        editingVisit = visit;
        visitFormHeading.setText("Chỉnh sửa lần khám");
        deleteVisitButton.setVisible(true);
        deleteVisitButton.setManaged(true);
        visitDateField.setValue(visit.getNgayKham());
        dentistField.setText(visit.getBacSiKham());
        symptomsField.setText(visit.getTrieuChung());
        diagnosisField.setText(visit.getChanDoan());
        treatmentField.setText(visit.getPhuongPhapDieuTri());
        visitNoteField.setText(nullToEmpty(visit.getGhiChu()));
        runOperation(() -> revenueService.getVisitRevenue(visit.getId()), revenues -> {
            clearRevenueRows();
            revenues.forEach(this::addRevenueRow);
            visitFormPane.setExpanded(true);
        }, "Không thể tải doanh thu của lần khám.");
    }

    private void addRevenueRow(Revenue revenue) {
        RevenueRow row = new RevenueRow(revenue);
        revenueRows.add(row);
        revenueRowsContainer.getChildren().add(row.container);
        updateRevenueTotal();
    }

    private void clearRevenueRows() {
        revenueRows.clear();
        revenueRowsContainer.getChildren().clear();
        updateRevenueTotal();
    }

    private void updateRevenueTotal() {
        revenueTotalLabel.setText(formatMoney(getRevenueTotal()));
    }

    private static String formatMoney(BigDecimal amount) {
        return NumberFormat.getNumberInstance(java.util.Locale.of("vi", "VN")).format(amount) + " đ";
    }

    private static VBox fieldBlock(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label(displayOptional(value));
        valueLabel.setWrapText(true);
        return new VBox(3, titleLabel, valueLabel);
    }

    private Patient patientFromForm() {
        Patient patient = new Patient(
                patientNameField.getText(),
                patientGenderField.getText(),
                patientBirthDateField.getValue()
        );
        patient.setGiayToTuyThan(emptyToNull(patientIdentityField.getText()));
        patient.setSoTheBhyt(emptyToNull(patientInsuranceField.getText()));
        patient.setDiaChi(emptyToNull(patientAddressField.getText()));
        patient.setNgheNghiep(emptyToNull(patientOccupationField.getText()));
        patient.setDanToc(emptyToNull(patientEthnicityField.getText()));
        return patient;
    }

    private static void copyIdentity(Patient source, Patient target) {
        target.setId(source.getId());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private void fillPatientForm(Patient patient) {
        patientNameField.setText(patient.getHoVaTen());
        patientGenderField.setText(patient.getGioiTinh());
        patientBirthDateField.setValue(patient.getNgaySinh());
        patientIdentityField.setText(nullToEmpty(patient.getGiayToTuyThan()));
        patientInsuranceField.setText(nullToEmpty(patient.getSoTheBhyt()));
        patientAddressField.setText(nullToEmpty(patient.getDiaChi()));
        patientOccupationField.setText(nullToEmpty(patient.getNgheNghiep()));
        patientEthnicityField.setText(nullToEmpty(patient.getDanToc()));
    }

    private void clearPatientForm() {
        patientNameField.clear();
        patientGenderField.clear();
        patientBirthDateField.setValue(null);
        patientIdentityField.clear();
        patientInsuranceField.clear();
        patientAddressField.clear();
        patientOccupationField.clear();
        patientEthnicityField.clear();
    }

    private void clearVisitForm() {
        visitDateField.setValue(null);
        dentistField.clear();
        symptomsField.clear();
        diagnosisField.clear();
        treatmentField.clear();
        visitNoteField.clear();
        clearRevenueRows();
    }

    private int nextVisitSequence() {
        return selectedPatientVisits.stream().mapToInt(Visit::getTt).max().orElse(0) + 1;
    }

    private void finishPatientSave(Patient savedPatient, String message) {
        selectedPatient = savedPatient;
        patientFormPane.setExpanded(false);
        clearPatientForm();
        showSuccess(message);
        loadPatients(searchField.getText());
        showPatient(savedPatient);
        showOnly(detailView);
    }

    private void updateSelectedCardState() {
        for (javafx.scene.Node node : patientListContainer.getChildren()) {
            node.getStyleClass().remove("patient-card-selected");
            if (node.getUserData() instanceof Patient patient
                    && selectedPatient != null
                    && Objects.equals(patient.getId(), selectedPatient.getId())) {
                node.getStyleClass().add("patient-card-selected");
            }
        }
    }

    private void showOnly(javafx.scene.Node visibleView) {
        setDisplayed(directoryView, directoryView == visibleView);
        setDisplayed(detailView, detailView == visibleView);
        setDisplayed(patientFormView, patientFormView == visibleView);
    }

    private static void setDisplayed(javafx.scene.Node node, boolean displayed) {
        node.setVisible(displayed);
        node.setManaged(displayed);
    }

    private <T> void runOperation(Supplier<T> operation, Consumer<T> onSuccess, String userError) {
        executor.execute(() -> {
            try {
                T result = operation.get();
                dispatchToUi(() -> onSuccess.accept(result));
            } catch (ServiceException | IllegalArgumentException | NullPointerException exception) {
                dispatchToUi(() -> showError(isValidationFailure(exception)
                        ? "Vui lòng nhập đầy đủ thông tin bắt buộc."
                        : userError));
            }
        });
    }

    private static boolean isValidationFailure(RuntimeException exception) {
        return exception instanceof IllegalArgumentException
                || exception instanceof NullPointerException
                || exception instanceof ServiceException && exception.getCause() == null;
    }

    private static void dispatchToUi(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private void showSuccess(String message) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().setAll("status-message", "status-success");
    }

    private void showError(String message) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().setAll("status-message", "status-error");
        listMessage.setText("");
    }

    private void clearStatus() {
        statusMessage.setText("");
    }

    private static String displayOptional(String value) {
        return value == null || value.isBlank() ? EMPTY_VALUE : value;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? EMPTY_VALUE : DATE_FORMAT.format(date);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private final class RevenueRow {
        private final Revenue source;
        private final HBox container = new HBox(8);
        private final TextField referenceField = new TextField();
        private final DatePicker dateField = new DatePicker();
        private final TextField descriptionField = new TextField();
        private final TextField amountField = new TextField();

        private RevenueRow(Revenue revenue) {
            source = revenue;
            referenceField.setPromptText("Số hiệu");
            referenceField.setText(nullToEmpty(revenue.getSoHieu()));
            referenceField.setPrefWidth(120);
            dateField.setValue(revenue.getNgayThang());
            dateField.setPrefWidth(145);
            descriptionField.setPromptText("Diễn giải");
            descriptionField.setText(revenue.getDienGiai());
            HBox.setHgrow(descriptionField, javafx.scene.layout.Priority.ALWAYS);
            amountField.setPromptText("Số tiền");
            amountField.setText(revenue.getSoTien().signum() == 0 ? "0" : revenue.getSoTien().toPlainString());
            amountField.setPrefWidth(135);
            amountField.textProperty().addListener((observable, previous, current) -> updateRevenueTotal());
            Button remove = new Button("Xóa");
            remove.getStyleClass().add("danger-button");
            remove.setOnAction(event -> {
                revenueRows.remove(this);
                revenueRowsContainer.getChildren().remove(container);
                updateRevenueTotal();
            });
            container.getStyleClass().add("revenue-row");
            container.getChildren().addAll(referenceField, dateField, descriptionField, amountField, remove);
        }

        private BigDecimal amountOrZero() {
            try {
                return amountField.getText() == null || amountField.getText().isBlank()
                        ? BigDecimal.ZERO
                        : new BigDecimal(amountField.getText().trim());
            } catch (NumberFormatException exception) {
                return BigDecimal.ZERO;
            }
        }

        private Revenue toRevenue() {
            if (dateField.getValue() == null || descriptionField.getText() == null
                    || descriptionField.getText().isBlank() || amountField.getText() == null
                    || amountField.getText().isBlank()) {
                throw new IllegalArgumentException("Missing revenue value");
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountField.getText().trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid revenue amount", exception);
            }
            Revenue revenue = new Revenue(
                    selectedPatient.getId(), dateField.getValue(), descriptionField.getText(), amount);
            revenue.setSoHieu(emptyToNull(referenceField.getText()));
            if (source.getId() != null) {
                revenue.setId(source.getId());
                revenue.setCreatedAt(source.getCreatedAt());
            }
            return revenue;
        }
    }
}
