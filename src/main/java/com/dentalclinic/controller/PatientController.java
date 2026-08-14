package com.dentalclinic.controller;

import com.dentalclinic.model.Patient;
import com.dentalclinic.model.PatientGender;
import com.dentalclinic.model.PatientPage;
import com.dentalclinic.model.PatientSearchCriteria;
import com.dentalclinic.model.Revenue;
import com.dentalclinic.model.Visit;
import com.dentalclinic.service.PatientService;
import com.dentalclinic.service.RevenueService;
import com.dentalclinic.service.ServiceException;
import com.dentalclinic.service.VisitService;
import com.dentalclinic.service.VisitRevenueWorkflowService;
import com.dentalclinic.util.DatePickerSupport;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PatientController {

    private static final String EMPTY_VALUE = "—";
    private static final Duration SEARCH_DEBOUNCE = Duration.millis(250);
    private static final Duration PATIENT_LOADING_MINIMUM = Duration.millis(450);
    private static final int PATIENT_PAGE_SIZE = 50;

    private final PatientService patientService;
    private final VisitService visitService;
    private final RevenueService revenueService;
    private final VisitRevenueWorkflowService workflowService;
    private final Executor executor;
    private final Duration patientLoadingMinimum;

    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, Boolean> patientSelectColumn;
    @FXML private TableColumn<Patient, String> patientCodeColumn;
    @FXML private TableColumn<Patient, String> patientNameColumn;
    @FXML private TableColumn<Patient, String> patientBirthDateColumn;
    @FXML private TableColumn<Patient, String> patientPhoneColumn;
    @FXML private TableColumn<Patient, Void> patientActionColumn;
    @FXML private Label patientTablePlaceholder;
    @FXML private Label listMessage;
    @FXML private VBox advancedFilterPanel;
    @FXML private TextField patientCodeFilterField;
    @FXML private TextField phoneFilterField;
    @FXML private DatePicker birthDateFilterField;
    @FXML private ComboBox<String> genderFilterField;
    @FXML private FlowPane activeFiltersContainer;
    @FXML private Button clearAllFiltersButton;
    @FXML private VBox patientSearchPanel;
    @FXML private Label directoryTitle;
    @FXML private Label directorySubtitle;
    @FXML private Button addPatientButton;
    @FXML private Button viewTrashButton;
    @FXML private Button bulkTrashButton;
    @FXML private Button closeTrashButton;
    @FXML private HBox trashActions;
    @FXML private Label trashSelectedCountLabel;
    @FXML private Button bulkRestoreButton;
    @FXML private Button bulkPermanentDeleteButton;
    @FXML private Label statusMessage;
    @FXML private VBox directoryView;
    @FXML private VBox detailView;
    @FXML private VBox patientFormView;
    @FXML private VBox patientLoadingView;
    @FXML private Label patientLoadingName;
    @FXML private Label selectedPatientName;
    @FXML private Label patientNameValue;
    @FXML private Label patientGenderValue;
    @FXML private Label patientBirthDateValue;
    @FXML private Label patientPhoneValue;
    @FXML private Label patientIdentityValue;
    @FXML private Label patientInsuranceValue;
    @FXML private Label patientAddressValue;
    @FXML private Label patientOccupationValue;
    @FXML private Label patientEthnicityValue;
    @FXML private Button editPatientButton;
    @FXML private Button addVisitButton;
    @FXML private Button deletePatientButton;
    @FXML private TitledPane patientFormPane;
    @FXML private Label patientFormTitle;
    @FXML private TextField patientNameField;
    @FXML private ComboBox<String> patientGenderField;
    @FXML private DatePicker patientBirthDateField;
    @FXML private TextField patientPhoneField;
    @FXML private TextField patientIdentityField;
    @FXML private TextField patientInsuranceField;
    @FXML private TextField patientAddressField;
    @FXML private TextField patientOccupationField;
    @FXML private TextField patientEthnicityField;
    @FXML private TitledPane visitFormPane;
    @FXML private DatePicker visitDateField;
    @FXML private Label visitDateError;
    @FXML private TextField dentistField;
    @FXML private Label dentistError;
    @FXML private TextArea symptomsField;
    @FXML private Label symptomsError;
    @FXML private TextArea diagnosisField;
    @FXML private Label diagnosisError;
    @FXML private TextArea treatmentField;
    @FXML private Label treatmentError;
    @FXML private TextArea visitNoteField;
    @FXML private VBox visitListContainer;
    @FXML private Label visitCountLabel;
    @FXML private VBox revenueRowsContainer;
    @FXML private Label revenueTotalLabel;
    @FXML private Label visitRevenueError;
    @FXML private Label visitFormHeading;
    @FXML private Button deleteVisitButton;
    @FXML private VBox tableLoadingOverlay;
    @FXML private Label tableLoadingLabel;
    @FXML private Label paginationStatusLabel;
    @FXML private Label pageNumberLabel;
    @FXML private Button firstPageButton;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button lastPageButton;

    private Patient selectedPatient;
    private List<Visit> selectedPatientVisits = List.of();
    private boolean creatingPatient;
    private Visit editingVisit;
    private final List<RevenueRow> revenueRows = new ArrayList<>();
    private final PauseTransition searchDebounce = new PauseTransition(SEARCH_DEBOUNCE);
    private long patientLoadVersion;
    private long patientDetailLoadVersion;
    private long visitEditorVersion;
    private boolean suppressCriteriaRefresh;
    private boolean showingTrash;
    private PauseTransition patientDetailDelay;
    private Patient pendingDetailPatient;
    private List<Visit> pendingDetailVisits;
    private boolean pendingDetailDataReady;
    private boolean pendingDetailDelayReady;
    private final Set<Long> selectedPatientIds = new HashSet<>();
    private final CheckBox selectAllPatientsCheckBox = new CheckBox();
    private int currentPageIndex;
    private int totalPages = 1;
    private long totalPatientCount;
    private boolean tableLoading;

    public PatientController(
            PatientService patientService,
            VisitService visitService,
            RevenueService revenueService,
            VisitRevenueWorkflowService workflowService
    ) {
        this(patientService, visitService, revenueService, workflowService,
                PatientController::runInBackground, PATIENT_LOADING_MINIMUM);
    }

    public PatientController(
            PatientService patientService,
            VisitService visitService,
            RevenueService revenueService,
            VisitRevenueWorkflowService workflowService,
            Executor executor
    ) {
        this(patientService, visitService, revenueService, workflowService, executor, PATIENT_LOADING_MINIMUM);
    }

    public PatientController(
            PatientService patientService,
            VisitService visitService,
            RevenueService revenueService,
            VisitRevenueWorkflowService workflowService,
            Executor executor,
            Duration patientLoadingMinimum
    ) {
        this.patientService = Objects.requireNonNull(patientService, "patientService must not be null");
        this.visitService = Objects.requireNonNull(visitService, "visitService must not be null");
        this.revenueService = Objects.requireNonNull(revenueService, "revenueService must not be null");
        this.workflowService = Objects.requireNonNull(workflowService, "workflowService must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.patientLoadingMinimum = Objects.requireNonNull(
                patientLoadingMinimum, "patientLoadingMinimum must not be null");
    }

    private static void runInBackground(Runnable command) {
        Thread thread = new Thread(command, "dental-ui-data");
        thread.setDaemon(true);
        thread.start();
    }

    private <T> void executeTask(Task<T> task, Consumer<T> onSuccess, Runnable onFailure) {
        executor.execute(() -> {
            try {
                task.run();
                T result = task.get();
                dispatchToUi(() -> onSuccess.accept(result));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                dispatchToUi(onFailure);
            } catch (ExecutionException | CancellationException exception) {
                dispatchToUi(onFailure);
            }
        });
    }

    @FXML
    private void initialize() {
        configureDatePickers();
        configureGenderSelectors();
        configureVisitValidation();
        configurePatientTable();
        configurePatientSearch();
        updateDirectoryMode();
        showDirectory();
        refreshPatients();
    }

    @FXML
    public void refreshPatients() {
        if (showingTrash) {
            loadDeletedPatients();
        } else {
            loadPatients(currentSearchCriteria());
        }
    }

    @FXML
    public void searchPatients() {
        if (showingTrash) {
            return;
        }
        searchDebounce.stop();
        loadPatients(currentSearchCriteria());
    }

    @FXML
    public void toggleAdvancedFilters() {
        boolean show = !advancedFilterPanel.isVisible();
        advancedFilterPanel.setVisible(show);
        advancedFilterPanel.setManaged(show);
    }

    @FXML
    public void clearAllSearchFilters() {
        suppressCriteriaRefresh = true;
        searchField.clear();
        patientCodeFilterField.clear();
        phoneFilterField.clear();
        birthDateFilterField.setValue(null);
        genderFilterField.setValue(null);
        suppressCriteriaRefresh = false;
        searchDebounce.stop();
        renderActiveFilterTags();
        loadPatients(PatientSearchCriteria.empty());
    }

    @FXML
    public void openTrash() {
        searchDebounce.stop();
        showingTrash = true;
        selectedPatient = null;
        patientTable.getSelectionModel().clearSelection();
        updateDirectoryMode();
        showDirectory();
        loadDeletedPatients();
    }

    @FXML
    public void closeTrash() {
        searchDebounce.stop();
        showingTrash = false;
        patientTable.getSelectionModel().clearSelection();
        updateDirectoryMode();
        showDirectory();
        loadPatients(currentSearchCriteria());
    }

    @FXML
    public void goToFirstPage() {
        loadRequestedPage(0);
    }

    @FXML
    public void goToPreviousPage() {
        loadRequestedPage(Math.max(0, currentPageIndex - 1));
    }

    @FXML
    public void goToNextPage() {
        loadRequestedPage(Math.min(totalPages - 1, currentPageIndex + 1));
    }

    @FXML
    public void goToLastPage() {
        loadRequestedPage(Math.max(0, totalPages - 1));
    }

    @FXML
    public void moveSelectedPatientsToTrash() {
        if (showingTrash || selectedPatientIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(selectedPatientIds);
        javafx.scene.control.Alert confirmation = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Chuyển " + ids.size() + " hồ sơ đã chọn vào Thùng rác? Các hồ sơ vẫn có thể khôi phục.",
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK
        );
        confirmation.setHeaderText("Xóa hàng loạt");
        if (confirmation.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) {
            return;
        }
        setTableLoading(true, "Đang chuyển hồ sơ vào Thùng rác...");
        executor.execute(() -> {
            try {
                patientService.deletePatients(ids);
                dispatchToUi(() -> {
                    clearBulkSelection();
                    showSuccess("Đã chuyển " + ids.size() + " hồ sơ vào Thùng rác.");
                    loadActivePatientPage(currentSearchCriteria(), currentPageIndex);
                });
            } catch (ServiceException | IllegalArgumentException exception) {
                dispatchToUi(() -> {
                    setTableLoading(false, null);
                    showError("Không thể chuyển các hồ sơ đã chọn vào Thùng rác.");
                });
            }
        });
    }

    @FXML
    public void restoreSelectedPatients() {
        if (!showingTrash || selectedPatientIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(selectedPatientIds);
        runTrashBulkOperation(ids,
                "Đang khôi phục các hồ sơ đã chọn...",
                patientService::restorePatients,
                "Đã khôi phục " + ids.size() + " hồ sơ bệnh nhân.",
                "Không thể khôi phục các hồ sơ đã chọn.");
    }

    @FXML
    public void permanentlyDeleteSelectedPatients() {
        if (!showingTrash || selectedPatientIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(selectedPatientIds);
        javafx.scene.control.Alert confirmation = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Hành động này sẽ xóa vĩnh viễn " + ids.size()
                        + " hồ sơ cùng toàn bộ lần khám và doanh thu liên quan.",
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK
        );
        confirmation.setHeaderText("Xóa vĩnh viễn các hồ sơ đã chọn?");
        if (confirmation.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) {
            return;
        }
        runTrashBulkOperation(ids,
                "Đang xóa vĩnh viễn các hồ sơ đã chọn...",
                patientService::permanentlyDeletePatients,
                "Đã xóa vĩnh viễn " + ids.size() + " hồ sơ bệnh nhân.",
                "Không thể xóa vĩnh viễn các hồ sơ đã chọn.");
    }

    private void runTrashBulkOperation(
            List<Long> ids,
            String loadingMessage,
            Consumer<List<Long>> operation,
            String successMessage,
            String errorMessage
    ) {
        setTableLoading(true, loadingMessage);
        executor.execute(() -> {
            try {
                operation.accept(ids);
                dispatchToUi(() -> {
                    clearBulkSelection();
                    showSuccess(successMessage);
                    loadDeletedPatientPage(currentPageIndex);
                });
            } catch (ServiceException | IllegalArgumentException exception) {
                dispatchToUi(() -> {
                    setTableLoading(false, null);
                    showError(errorMessage);
                });
            }
        });
    }

    @FXML
    public void moveSelectedPatientToTrash() {
        if (selectedPatient == null) {
            showError("Vui lòng chọn bệnh nhân cần xóa.");
            return;
        }
        javafx.scene.control.Alert confirmation = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Hồ sơ sẽ được chuyển vào Thùng rác và có thể khôi phục sau.",
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK
        );
        confirmation.setHeaderText("Chuyển hồ sơ vào Thùng rác?");
        if (confirmation.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) {
            return;
        }
        Long patientId = selectedPatient.getId();
        runOperation(() -> {
            patientService.deletePatient(patientId);
            return patientId;
        }, ignored -> {
            selectedPatient = null;
            showingTrash = false;
            updateDirectoryMode();
            showDirectory();
            loadPatients(currentSearchCriteria());
            showSuccess("Đã chuyển hồ sơ vào Thùng rác.");
        }, "Không thể chuyển hồ sơ vào Thùng rác.");
    }

    @FXML
    public void restoreSelectedPatient() {
        Patient patient = patientTable.getSelectionModel().getSelectedItem();
        if (!showingTrash || patient == null) {
            showError("Vui lòng chọn hồ sơ cần khôi phục.");
            return;
        }
        runOperation(() -> {
            patientService.restorePatient(patient.getId());
            return patient.getId();
        }, ignored -> {
            loadDeletedPatientPage(currentPageIndex);
            showSuccess("Đã khôi phục hồ sơ bệnh nhân.");
        }, "Không thể khôi phục hồ sơ bệnh nhân.");
    }

    @FXML
    public void permanentlyDeleteSelectedPatient() {
        Patient patient = patientTable.getSelectionModel().getSelectedItem();
        if (!showingTrash || patient == null) {
            showError("Vui lòng chọn hồ sơ cần xóa vĩnh viễn.");
            return;
        }
        javafx.scene.control.Alert confirmation = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Hành động này sẽ xóa vĩnh viễn hồ sơ, toàn bộ lần khám và doanh thu liên quan.",
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK
        );
        confirmation.setHeaderText("Xóa vĩnh viễn " + patient.getHoVaTen() + "?");
        if (confirmation.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) {
            return;
        }
        runOperation(() -> {
            patientService.permanentlyDeletePatient(patient.getId());
            return patient.getId();
        }, ignored -> {
            loadDeletedPatientPage(currentPageIndex);
            showSuccess("Đã xóa vĩnh viễn hồ sơ bệnh nhân.");
        }, "Không thể xóa vĩnh viễn hồ sơ bệnh nhân.");
    }

    @FXML
    public void openNewPatientForm() {
        resetVisitEditor();
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
        resetVisitEditor();
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
        try {
            DatePickerSupport.commit(patientBirthDateField);
        } catch (DateTimeParseException exception) {
            showError("Ngày sinh không hợp lệ. Hãy nhập theo Ngày/Tháng/Năm.");
            return;
        }
        if (patientBirthDateField.getValue() == null
                || patientGenderField.getValue() == null
                || patientNameField.getText() == null
                || patientNameField.getText().isBlank()) {
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
        cancelPatientDetailLoad();
        resetVisitEditor();
        selectedPatient = null;
        selectedPatientVisits = List.of();
        patientTable.getSelectionModel().clearSelection();
        showOnly(directoryView);
        clearStatus();
    }

    @FXML
    public void openVisitForm() {
        if (selectedPatient == null) {
            showError("Vui lòng chọn bệnh nhân trước khi thêm lần khám.");
            return;
        }
        resetVisitEditor();
        visitFormHeading.setText("Thêm lần khám");
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
        LocalDate visitDate = validatedVisitDate();
        boolean valid = visitDate != null;
        valid &= validateDentist();
        valid &= validateRequiredField(symptomsField, symptomsError, "Vui lòng nhập triệu chứng.");
        valid &= validateRequiredField(diagnosisField, diagnosisError, "Vui lòng nhập chẩn đoán.");
        valid &= validateRequiredField(treatmentField, treatmentError, "Vui lòng nhập phương pháp điều trị.");
        boolean revenuesValid = true;
        for (RevenueRow row : revenueRows) {
            revenuesValid &= row.validate();
        }
        setStandaloneError(visitRevenueError, revenuesValid ? null : "Vui lòng sửa các dòng doanh thu được đánh dấu.");
        valid &= revenuesValid;
        if (!valid) {
            showError("Vui lòng sửa các trường được đánh dấu trước khi lưu.");
            return;
        }

        Visit editingSnapshot = editingVisit;
        boolean creatingVisit = editingSnapshot == null;
        Visit visit = new Visit(
                selectedPatient.getId(),
                creatingVisit ? nextVisitSequence() : editingSnapshot.getTt(),
                visitDate,
                symptomsField.getText().trim(),
                diagnosisField.getText().trim(),
                treatmentField.getText().trim(),
                dentistField.getText().trim()
        );
        visit.setGhiChu(emptyToNull(visitNoteField.getText()));
        if (!creatingVisit) {
            visit.setId(editingSnapshot.getId());
            visit.setCreatedAt(editingSnapshot.getCreatedAt());
            visit.setUpdatedAt(editingSnapshot.getUpdatedAt());
        }
        List<Revenue> revenues = revenueRows.stream().map(RevenueRow::toRevenue).toList();
        Long patientId = selectedPatient.getId();
        long editorVersion = visitEditorVersion;
        runOperation(
                () -> {
                    if (creatingVisit) {
                        workflowService.create(visit, revenues);
                    } else {
                        workflowService.update(visit, revenues);
                    }
                    return visitService.getPatientVisits(patientId);
                },
                visits -> {
                    if (editorVersion != visitEditorVersion || !isSelectedPatient(patientId)) {
                        return;
                    }
                    selectedPatientVisits = List.copyOf(visits);
                    renderVisits(visits);
                    resetVisitEditor();
                    showSuccess("Đã lưu lần khám thành công.");
                },
                "Không thể lưu lần khám."
        );
    }

    @FXML
    public void cancelVisitForm() {
        resetVisitEditor();
        clearStatus();
    }

    @FXML
    public void addRevenueRow() {
        Revenue revenue = new Revenue(0L,
                visitDateField.getValue() == null ? LocalDate.now() : visitDateField.getValue(),
                emptyToNull(symptomsField.getText()), BigDecimal.ZERO);
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
        long editorVersion = visitEditorVersion;
        runOperation(() -> {
            workflowService.delete(visitId);
            return visitService.getPatientVisits(patientId);
        }, visits -> {
            if (editorVersion != visitEditorVersion || !isSelectedPatient(patientId)) {
                return;
            }
            selectedPatientVisits = List.copyOf(visits);
            renderVisits(visits);
            resetVisitEditor();
            showSuccess("Đã xóa lần khám.");
        }, "Không thể xóa lần khám.");
    }

    public Patient getSelectedPatient() {
        return selectedPatient;
    }

    public int getDisplayedPatientCount() {
        return patientTable.getItems().size();
    }

    public int getDisplayedVisitCount() {
        return visitListContainer.getChildren().size();
    }

    public int getSelectedPatientVisitCount() {
        return selectedPatientVisits.size();
    }

    public int getRevenueRowCount() {
        return revenueRows.size();
    }

    public BigDecimal getRevenueTotal() {
        return revenueRows.stream()
                .map(RevenueRow::amountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void configurePatientTable() {
        VBox.setVgrow(patientTable, Priority.ALWAYS);
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        patientSelectColumn.setText(null);
        patientSelectColumn.getStyleClass().add("checkbox-column");
        patientActionColumn.getStyleClass().add("action-column");
        selectAllPatientsCheckBox.setAccessibleText("Chọn tất cả bệnh nhân trên trang hiện tại");
        selectAllPatientsCheckBox.setFocusTraversable(false);
        selectAllPatientsCheckBox.setOnAction(event -> {
            if (selectAllPatientsCheckBox.isSelected()) {
                patientTable.getItems().stream().map(Patient::getId)
                        .filter(Objects::nonNull).forEach(selectedPatientIds::add);
            } else {
                patientTable.getItems().stream().map(Patient::getId)
                        .forEach(selectedPatientIds::remove);
            }
            patientTable.refresh();
            updateBulkSelectionControls();
        });
        patientSelectColumn.setGraphic(selectAllPatientsCheckBox);
        patientSelectColumn.setCellValueFactory(cell ->
                new SimpleBooleanProperty(selectedPatientIds.contains(cell.getValue().getId())));
        patientSelectColumn.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                setAlignment(Pos.CENTER);
                setPadding(new Insets(5, 8, 5, 8));
                checkBox.setFocusTraversable(false);
                checkBox.setOnAction(event -> {
                    Patient patient = getTableRow() == null ? null : getTableRow().getItem();
                    if (patient != null && patient.getId() != null) {
                        if (checkBox.isSelected()) {
                            selectedPatientIds.add(patient.getId());
                        } else {
                            selectedPatientIds.remove(patient.getId());
                        }
                        updateBulkSelectionControls();
                    }
                    event.consume();
                });
            }

            @Override
            protected void updateItem(Boolean selected, boolean empty) {
                super.updateItem(selected, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                checkBox.setSelected(Boolean.TRUE.equals(selected));
                checkBox.setDisable(tableLoading);
                setGraphic(checkBox);
                setText(null);
            }
        });
        patientCodeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatPatientCode(cell.getValue().getId())));
        patientNameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getHoVaTen()));
        patientBirthDateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatDate(cell.getValue().getNgaySinh())));
        patientPhoneColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(displayOptional(cell.getValue().getSoDienThoai())));
        patientActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editOrRestore = new Button();
            private final Button delete = new Button("Xóa");
            private final HBox actions = new HBox(6, editOrRestore, delete);
            {
                setAlignment(Pos.CENTER);
                setPadding(new Insets(5, 8, 5, 8));
                actions.setAlignment(Pos.CENTER);
                editOrRestore.setOnAction(event -> {
                    Patient patient = getTableRow() == null ? null : getTableRow().getItem();
                    if (patient != null) {
                        if (showingTrash) {
                            patientTable.getSelectionModel().select(patient);
                            restoreSelectedPatient();
                        } else {
                            openPatientEditFromTable(patient);
                        }
                    }
                    event.consume();
                });
                delete.getStyleClass().add("table-action-delete");
                delete.setOnAction(event -> {
                    Patient patient = getTableRow() == null ? null : getTableRow().getItem();
                    if (patient != null) {
                        patientTable.getSelectionModel().select(patient);
                        if (showingTrash) {
                            permanentlyDeleteSelectedPatient();
                        } else {
                            selectedPatient = patient;
                            moveSelectedPatientToTrash();
                        }
                    }
                    event.consume();
                });
            }

            @Override
            protected void updateItem(Void ignored, boolean empty) {
                super.updateItem(ignored, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                editOrRestore.setText(showingTrash ? "Khôi phục" : "Sửa");
                editOrRestore.getStyleClass().setAll(showingTrash
                        ? "table-action-restore" : "table-action-edit");
                delete.setText(showingTrash ? "Xóa vĩnh viễn" : "Xóa");
                setGraphic(actions);
                setText(null);
            }
        });
        patientTable.setRowFactory(table -> {
            TableRow<Patient> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && !isInteractiveTableTarget(event.getTarget(), row)) {
                    patientTable.getSelectionModel().select(row.getItem());
                    if (!showingTrash) {
                        selectPatient(row.getItem());
                    }
                }
            });
            return row;
        });
    }

    private void openPatientEditFromTable(Patient patient) {
        resetVisitEditor();
        selectedPatient = patient;
        selectedPatientVisits = List.of();
        creatingPatient = false;
        fillPatientForm(patient);
        patientFormTitle.setText("Chỉnh sửa thông tin bệnh nhân");
        patientFormPane.setText("Chỉnh sửa thông tin bệnh nhân");
        patientFormPane.setExpanded(true);
        showOnly(patientFormView);
        clearStatus();
    }

    private static boolean isInteractiveTableTarget(Object target, TableRow<Patient> row) {
        Node node = target instanceof Node targetNode ? targetNode : null;
        while (node != null && node != row) {
            if (node instanceof ButtonBase) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void configureDatePickers() {
        DatePickerSupport.configure(birthDateFilterField);
        DatePickerSupport.configure(patientBirthDateField);
        DatePickerSupport.configure(visitDateField);
    }

    private void configureGenderSelectors() {
        List<String> genders = PatientGender.displayNames();
        genderFilterField.setItems(FXCollections.observableArrayList(genders));
        patientGenderField.setItems(FXCollections.observableArrayList(genders));
    }

    private void configureVisitValidation() {
        configureRequiredField(dentistField, dentistError, "Vui lòng nhập bác sĩ khám.");
        configureRequiredField(symptomsField, symptomsError, "Vui lòng nhập triệu chứng.");
        symptomsField.textProperty().addListener((observable, previous, current) ->
                revenueRows.forEach(row -> row.syncDescriptionFromSymptoms(current)));
        configureRequiredField(diagnosisField, diagnosisError, "Vui lòng nhập chẩn đoán.");
        configureRequiredField(treatmentField, treatmentError, "Vui lòng nhập phương pháp điều trị.");
        visitDateField.getEditor().textProperty().addListener((observable, previous, current) -> {
            if (current == null || current.isBlank()) {
                clearFieldError(visitDateField, visitDateError);
                return;
            }
            if (current.trim().length() >= DatePickerSupport.DATE_PATTERN.length()) {
                validateVisitDateText(false);
            }
        });
        visitDateField.getEditor().focusedProperty().addListener((observable, hadFocus, hasFocus) -> {
            if (!hasFocus && !visitDateField.getEditor().getText().isBlank()) {
                validateVisitDateText(false);
            }
        });
        dentistField.textProperty().addListener((observable, previous, current) -> {
            if (current != null && current.trim().length() > 200) {
                setFieldError(dentistField, dentistError, "Tên bác sĩ tối đa 200 ký tự.");
            }
        });
    }

    private void configureRequiredField(TextInputControl field, Label errorLabel, String message) {
        field.textProperty().addListener((observable, previous, current) -> {
            if (current != null && !current.isBlank()) {
                clearFieldError(field, errorLabel);
            }
        });
        field.focusedProperty().addListener((observable, hadFocus, hasFocus) -> {
            if (!hasFocus && (field.getText() == null || field.getText().isBlank())) {
                setFieldError(field, errorLabel, message);
            }
        });
    }

    private LocalDate validatedVisitDate() {
        return validateVisitDateText(true);
    }

    private LocalDate validateVisitDateText(boolean required) {
        String value = visitDateField.getEditor().getText();
        if (value == null || value.isBlank()) {
            if (required) {
                setFieldError(visitDateField, visitDateError, "Vui lòng nhập ngày khám.");
            } else {
                clearFieldError(visitDateField, visitDateError);
            }
            visitDateField.setValue(null);
            return null;
        }
        try {
            LocalDate date = DatePickerSupport.commit(visitDateField);
            if (date.isAfter(LocalDate.now())) {
                setFieldError(visitDateField, visitDateError,
                        "Ngày khám không được nằm trong tương lai.");
                return null;
            }
            visitDateField.setValue(date);
            clearFieldError(visitDateField, visitDateError);
            return date;
        } catch (DateTimeParseException exception) {
            setFieldError(visitDateField, visitDateError,
                    "Ngày không hợp lệ. Hãy nhập theo Ngày/Tháng/Năm.");
            return null;
        }
    }

    private static boolean validateRequiredField(
            TextInputControl field, Label errorLabel, String message) {
        if (field.getText() == null || field.getText().isBlank()) {
            setFieldError(field, errorLabel, message);
            return false;
        }
        clearFieldError(field, errorLabel);
        return true;
    }

    private boolean validateDentist() {
        if (!validateRequiredField(dentistField, dentistError, "Vui lòng nhập bác sĩ khám.")) {
            return false;
        }
        if (dentistField.getText().trim().length() > 200) {
            setFieldError(dentistField, dentistError, "Tên bác sĩ tối đa 200 ký tự.");
            return false;
        }
        return true;
    }

    private static void setFieldError(Control field, Label errorLabel, String message) {
        if (!field.getStyleClass().contains("input-invalid")) {
            field.getStyleClass().add("input-invalid");
        }
        setStandaloneError(errorLabel, message);
    }

    private static void clearFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("input-invalid");
        setStandaloneError(errorLabel, null);
    }

    private static void setStandaloneError(Label label, String message) {
        boolean visible = message != null && !message.isBlank();
        label.setText(visible ? message : "");
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void clearVisitValidationErrors() {
        clearFieldError(visitDateField, visitDateError);
        clearFieldError(dentistField, dentistError);
        clearFieldError(symptomsField, symptomsError);
        clearFieldError(diagnosisField, diagnosisError);
        clearFieldError(treatmentField, treatmentError);
        setStandaloneError(visitRevenueError, null);
    }

    private void configurePatientSearch() {
        searchDebounce.setOnFinished(event -> {
            if (!showingTrash) {
                loadPatients(currentSearchCriteria());
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> requestPatientSearch());
        patientCodeFilterField.textProperty().addListener((observable, oldValue, newValue) -> requestPatientSearch());
        phoneFilterField.textProperty().addListener((observable, oldValue, newValue) -> requestPatientSearch());
        birthDateFilterField.valueProperty().addListener((observable, oldValue, newValue) -> requestPatientSearch());
        genderFilterField.valueProperty().addListener((observable, oldValue, newValue) -> requestPatientSearch());
        renderActiveFilterTags();
    }

    private void requestPatientSearch() {
        if (suppressCriteriaRefresh || showingTrash) {
            return;
        }
        patientLoadVersion++;
        renderActiveFilterTags();
        searchDebounce.playFromStart();
    }

    private PatientSearchCriteria currentSearchCriteria() {
        return new PatientSearchCriteria(
                searchField.getText(),
                patientCodeFilterField.getText(),
                phoneFilterField.getText(),
                birthDateFilterField.getValue(),
                genderFilterField.getValue()
        );
    }

    private void renderActiveFilterTags() {
        activeFiltersContainer.getChildren().clear();
        PatientSearchCriteria criteria = currentSearchCriteria();
        if (criteria.patientCode() != null) {
            addFilterTag("Mã BN: " + criteria.patientCode(), patientCodeFilterField::clear);
        }
        if (criteria.phone() != null) {
            addFilterTag("SĐT: " + criteria.phone(), phoneFilterField::clear);
        }
        if (criteria.birthDate() != null) {
            addFilterTag("Ngày sinh: " + formatDate(criteria.birthDate()), () -> birthDateFilterField.setValue(null));
        }
        if (criteria.gender() != null) {
            addFilterTag("Giới tính: " + criteria.gender(), () -> genderFilterField.setValue(null));
        }
        boolean hasCriteria = !criteria.isEmpty();
        clearAllFiltersButton.setVisible(hasCriteria);
        clearAllFiltersButton.setManaged(hasCriteria);
        activeFiltersContainer.setVisible(criteria.hasAdvancedFilters());
        activeFiltersContainer.setManaged(criteria.hasAdvancedFilters());
    }

    private void addFilterTag(String text, Runnable clearAction) {
        Button tag = new Button(text + "  ×");
        tag.getStyleClass().add("filter-tag");
        tag.setOnAction(event -> clearAction.run());
        activeFiltersContainer.getChildren().add(tag);
    }

    private void loadPatients(PatientSearchCriteria criteria) {
        currentPageIndex = 0;
        loadActivePatientPage(criteria, 0);
    }

    private void loadActivePatientPage(PatientSearchCriteria criteria, int pageIndex) {
        long requestVersion = ++patientLoadVersion;
        setTableLoading(true, "Đang tải danh sách bệnh nhân...");
        clearBulkSelection();
        Task<PatientPage> loadTask = new Task<>() {
            @Override
            protected PatientPage call() {
                return patientService.searchPatientPage(criteria, pageIndex, PATIENT_PAGE_SIZE);
            }
        };
        executeTask(loadTask, page -> {
            if (requestVersion == patientLoadVersion && !showingTrash) {
                setTableLoading(false, null);
                renderPatientPage(page, criteria);
            }
        }, () -> {
            if (requestVersion == patientLoadVersion && !showingTrash) {
                setTableLoading(false, null);
                showError("Không thể tải danh sách bệnh nhân.");
            }
        });
    }

    private void loadDeletedPatients() {
        currentPageIndex = 0;
        loadDeletedPatientPage(0);
    }

    private void loadDeletedPatientPage(int pageIndex) {
        long requestVersion = ++patientLoadVersion;
        setTableLoading(true, "Đang tải Thùng rác...");
        clearBulkSelection();
        Task<PatientPage> loadTask = new Task<>() {
            @Override
            protected PatientPage call() {
                return patientService.getDeletedPatientPage(pageIndex, PATIENT_PAGE_SIZE);
            }
        };
        executeTask(loadTask, page -> {
            if (requestVersion == patientLoadVersion && showingTrash) {
                setTableLoading(false, null);
                renderPatientPage(page, PatientSearchCriteria.empty());
                patientTablePlaceholder.setText("Thùng rác trống");
            }
        }, () -> {
            if (requestVersion == patientLoadVersion && showingTrash) {
                setTableLoading(false, null);
                showError("Không thể tải Thùng rác.");
            }
        });
    }

    private void updateDirectoryMode() {
        directoryTitle.setText(showingTrash ? "Thùng rác" : "Bệnh nhân");
        directorySubtitle.setText(showingTrash
                ? "Hồ sơ đã xóa tạm thời — có thể khôi phục hoặc xóa vĩnh viễn"
                : "Quản lý thông tin và hồ sơ khám bệnh");
        patientSearchPanel.setVisible(!showingTrash);
        patientSearchPanel.setManaged(!showingTrash);
        addPatientButton.setVisible(!showingTrash);
        addPatientButton.setManaged(!showingTrash);
        viewTrashButton.setVisible(!showingTrash);
        viewTrashButton.setManaged(!showingTrash);
        bulkTrashButton.setVisible(!showingTrash);
        bulkTrashButton.setManaged(!showingTrash);
        closeTrashButton.setVisible(showingTrash);
        closeTrashButton.setManaged(showingTrash);
        patientSelectColumn.setVisible(true);
        selectAllPatientsCheckBox.setDisable(tableLoading);
        double actionWidth = showingTrash ? 235 : 150;
        patientActionColumn.setMinWidth(actionWidth);
        patientActionColumn.setPrefWidth(actionWidth);
        patientActionColumn.setMaxWidth(actionWidth);
        patientTable.refresh();
        updateBulkSelectionControls();
    }

    private void renderPatientPage(PatientPage page, PatientSearchCriteria criteria) {
        currentPageIndex = page.pageIndex();
        totalPages = page.totalPages();
        totalPatientCount = page.totalElements();
        patientTable.setItems(FXCollections.observableArrayList(page.patients()));
        patientTablePlaceholder.setText(criteria.isEmpty()
                ? "Chưa có bệnh nhân"
                : "Không tìm thấy kết quả");
        updatePaginationControls(page);
        clearBulkSelection();
    }

    private void loadRequestedPage(int pageIndex) {
        if (tableLoading || pageIndex == currentPageIndex || pageIndex < 0 || pageIndex >= totalPages) {
            return;
        }
        if (showingTrash) {
            loadDeletedPatientPage(pageIndex);
        } else {
            loadActivePatientPage(currentSearchCriteria(), pageIndex);
        }
    }

    private void updatePaginationControls(PatientPage page) {
        String first = formatCount(page.firstDisplayedNumber());
        String last = formatCount(page.lastDisplayedNumber());
        String total = formatCount(page.totalElements());
        paginationStatusLabel.setText("Hiển thị " + first + " - " + last
                + " trên tổng số " + total + " bệnh nhân");
        pageNumberLabel.setText("Trang " + (page.pageIndex() + 1) + " / " + page.totalPages());
        updatePaginationButtonState();
    }

    private void updatePaginationButtonState() {
        boolean first = currentPageIndex <= 0;
        boolean last = currentPageIndex >= totalPages - 1;
        firstPageButton.setDisable(tableLoading || first);
        previousPageButton.setDisable(tableLoading || first);
        nextPageButton.setDisable(tableLoading || last);
        lastPageButton.setDisable(tableLoading || last);
    }

    private void setTableLoading(boolean loading, String message) {
        tableLoading = loading;
        tableLoadingOverlay.setVisible(loading);
        tableLoadingOverlay.setManaged(loading);
        tableLoadingLabel.setText(message == null ? "Đang tải dữ liệu..." : message);
        patientTable.setDisable(loading);
        selectAllPatientsCheckBox.setDisable(loading);
        updatePaginationButtonState();
        updateBulkSelectionControls();
    }

    private void clearBulkSelection() {
        selectedPatientIds.clear();
        selectAllPatientsCheckBox.setSelected(false);
        selectAllPatientsCheckBox.setIndeterminate(false);
        patientTable.refresh();
        updateBulkSelectionControls();
    }

    private void updateBulkSelectionControls() {
        long selectedOnPage = patientTable.getItems().stream()
                .map(Patient::getId).filter(selectedPatientIds::contains).count();
        int rows = patientTable.getItems().size();
        selectAllPatientsCheckBox.setSelected(rows > 0 && selectedOnPage == rows);
        selectAllPatientsCheckBox.setIndeterminate(selectedOnPage > 0 && selectedOnPage < rows);
        bulkTrashButton.setDisable(showingTrash || tableLoading || selectedPatientIds.isEmpty());
        bulkTrashButton.setText(selectedPatientIds.isEmpty()
                ? "Thùng rác" : "Thùng rác (" + selectedPatientIds.size() + ")");
        boolean showTrashBulkActions = showingTrash && !selectedPatientIds.isEmpty();
        trashActions.setVisible(showTrashBulkActions);
        trashActions.setManaged(showTrashBulkActions);
        trashSelectedCountLabel.setText("Đã chọn " + selectedPatientIds.size() + " hồ sơ");
        bulkRestoreButton.setDisable(tableLoading || !showTrashBulkActions);
        bulkPermanentDeleteButton.setDisable(tableLoading || !showTrashBulkActions);
    }

    private static String formatCount(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private void showListMessage(String message) {
        listMessage.setText(message);
        listMessage.setVisible(true);
        listMessage.setManaged(true);
    }

    private void hideListMessage() {
        listMessage.setText("");
        listMessage.setVisible(false);
        listMessage.setManaged(false);
    }

    private void selectPatient(Patient patient) {
        if (patient == null) {
            return;
        }
        cancelPatientDetailLoad();
        resetVisitEditor();
        selectedPatientVisits = List.of();
        visitListContainer.getChildren().clear();
        visitCountLabel.setText("0 lần khám");
        long requestVersion = patientDetailLoadVersion;
        pendingDetailPatient = patient;
        pendingDetailVisits = List.of();
        pendingDetailDataReady = false;
        pendingDetailDelayReady = patientLoadingMinimum.toMillis() <= 0;
        selectedPatient = null;
        patientTable.getSelectionModel().select(patient);
        patientLoadingName.setText(patient.getHoVaTen());
        showOnly(patientLoadingView);

        if (!pendingDetailDelayReady) {
            patientDetailDelay = new PauseTransition(patientLoadingMinimum);
            patientDetailDelay.setOnFinished(event -> {
                if (requestVersion == patientDetailLoadVersion) {
                    pendingDetailDelayReady = true;
                    showPendingPatientDetailIfReady(requestVersion);
                }
            });
            patientDetailDelay.play();
        }

        executor.execute(() -> {
            try {
                List<Visit> visits = visitService.getPatientVisits(patient.getId());
                dispatchToUi(() -> {
                    if (requestVersion == patientDetailLoadVersion) {
                        pendingDetailVisits = List.copyOf(visits);
                        pendingDetailDataReady = true;
                        showPendingPatientDetailIfReady(requestVersion);
                    }
                });
            } catch (ServiceException | IllegalArgumentException | NullPointerException exception) {
                dispatchToUi(() -> {
                    if (requestVersion == patientDetailLoadVersion) {
                        showDirectory();
                        showError("Không thể tải hồ sơ và lịch sử khám.");
                    }
                });
            }
        });
    }

    private void showPendingPatientDetailIfReady(long requestVersion) {
        if (requestVersion != patientDetailLoadVersion
                || !pendingDetailDataReady
                || !pendingDetailDelayReady
                || pendingDetailPatient == null) {
            return;
        }
        selectedPatient = pendingDetailPatient;
        selectedPatientVisits = pendingDetailVisits;
        showPatient(selectedPatient);
        renderVisits(selectedPatientVisits);
        showOnly(detailView);
        pendingDetailPatient = null;
        pendingDetailVisits = List.of();
    }

    private void cancelPatientDetailLoad() {
        patientDetailLoadVersion++;
        if (patientDetailDelay != null) {
            patientDetailDelay.stop();
            patientDetailDelay = null;
        }
        pendingDetailPatient = null;
        pendingDetailVisits = List.of();
        pendingDetailDataReady = false;
        pendingDetailDelayReady = false;
    }

    private void showPatient(Patient patient) {
        boolean hasPatient = patient != null;
        editPatientButton.setDisable(!hasPatient);
        addVisitButton.setDisable(!hasPatient);
        deletePatientButton.setDisable(!hasPatient);
        if (!hasPatient) {
            return;
        }
        selectedPatientName.setText(patient.getHoVaTen());
        patientNameValue.setText(patient.getHoVaTen());
        patientGenderValue.setText(patient.getGioiTinh());
        patientBirthDateValue.setText(formatDate(patient.getNgaySinh()));
        patientPhoneValue.setText(displayOptional(patient.getSoDienThoai()));
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
        if (selectedPatient == null || !Objects.equals(visit.getPatientId(), selectedPatient.getId())) {
            showError("Lần khám này không thuộc hồ sơ bệnh nhân đang mở.");
            return;
        }
        resetVisitEditor();
        editingVisit = visit;
        Long patientId = selectedPatient.getId();
        long editorVersion = visitEditorVersion;
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
            if (editorVersion != visitEditorVersion || !isSelectedPatient(patientId)
                    || editingVisit == null || !Objects.equals(editingVisit.getId(), visit.getId())) {
                return;
            }
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
                patientGenderField.getValue(),
                patientBirthDateField.getValue()
        );
        patient.setGiayToTuyThan(emptyToNull(patientIdentityField.getText()));
        patient.setSoDienThoai(emptyToNull(patientPhoneField.getText()));
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
        patientGenderField.setValue(patient.getGioiTinh());
        patientBirthDateField.setValue(patient.getNgaySinh());
        patientPhoneField.setText(nullToEmpty(patient.getSoDienThoai()));
        patientIdentityField.setText(nullToEmpty(patient.getGiayToTuyThan()));
        patientInsuranceField.setText(nullToEmpty(patient.getSoTheBhyt()));
        patientAddressField.setText(nullToEmpty(patient.getDiaChi()));
        patientOccupationField.setText(nullToEmpty(patient.getNgheNghiep()));
        patientEthnicityField.setText(nullToEmpty(patient.getDanToc()));
    }

    private void clearPatientForm() {
        patientNameField.clear();
        patientGenderField.setValue(null);
        patientBirthDateField.setValue(null);
        patientPhoneField.clear();
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
        clearVisitValidationErrors();
    }

    private void resetVisitEditor() {
        visitEditorVersion++;
        editingVisit = null;
        visitFormPane.setExpanded(false);
        visitFormHeading.setText("Thêm lần khám");
        deleteVisitButton.setVisible(false);
        deleteVisitButton.setManaged(false);
        clearVisitForm();
    }

    private int nextVisitSequence() {
        return selectedPatientVisits.stream().mapToInt(Visit::getTt).max().orElse(0) + 1;
    }

    private void finishPatientSave(Patient savedPatient, String message) {
        boolean createdPatient = creatingPatient;
        resetVisitEditor();
        selectedPatient = savedPatient;
        if (createdPatient) {
            selectedPatientVisits = List.of();
        }
        creatingPatient = false;
        patientFormPane.setExpanded(false);
        clearPatientForm();
        showSuccess(message);
        loadPatients(currentSearchCriteria());
        if (!createdPatient) {
            selectPatient(savedPatient);
            return;
        }
        showPatient(savedPatient);
        renderVisits(selectedPatientVisits);
        showOnly(detailView);
    }

    private boolean isSelectedPatient(Long patientId) {
        return selectedPatient != null && Objects.equals(selectedPatient.getId(), patientId);
    }

    private void showOnly(javafx.scene.Node visibleView) {
        setDisplayed(directoryView, directoryView == visibleView);
        setDisplayed(detailView, detailView == visibleView);
        setDisplayed(patientFormView, patientFormView == visibleView);
        setDisplayed(patientLoadingView, patientLoadingView == visibleView);
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
        hideListMessage();
    }

    private void clearStatus() {
        statusMessage.setText("");
    }

    private static String displayOptional(String value) {
        return value == null || value.isBlank() ? EMPTY_VALUE : value;
    }

    private static String formatPatientCode(Long id) {
        return id == null ? EMPTY_VALUE : "BN-%06d".formatted(id);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? EMPTY_VALUE : DatePickerSupport.format(date);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private final class RevenueRow {
        private final Revenue source;
        private final VBox container = new VBox(4);
        private final HBox fields = new HBox(8);
        private final TextField referenceField = new TextField();
        private final DatePicker dateField = new DatePicker();
        private final TextField descriptionField = new TextField();
        private final TextField amountField = new TextField();
        private final Label errorLabel = new Label();
        private boolean autoFillDescription;
        private boolean syncingDescription;

        private RevenueRow(Revenue revenue) {
            source = revenue;
            referenceField.setPromptText("Số hiệu");
            referenceField.setText(nullToEmpty(revenue.getSoHieu()));
            referenceField.setPrefWidth(120);
            DatePickerSupport.configure(dateField);
            dateField.setValue(revenue.getNgayThang());
            dateField.setPrefWidth(145);
            descriptionField.setPromptText("Diễn giải");
            descriptionField.setText(nullToEmpty(revenue.getDienGiai()));
            autoFillDescription = revenue.getId() == null;
            HBox.setHgrow(descriptionField, javafx.scene.layout.Priority.ALWAYS);
            amountField.setPromptText("Số tiền");
            amountField.setText(revenue.getSoTien().signum() == 0 ? "0" : revenue.getSoTien().toPlainString());
            amountField.setPrefWidth(135);
            amountField.textProperty().addListener((observable, previous, current) -> {
                updateRevenueTotal();
                if (current != null && !current.isBlank()) {
                    validate(false);
                }
            });
            descriptionField.textProperty().addListener((observable, previous, current) -> {
                if (!syncingDescription) {
                    autoFillDescription = false;
                }
            });
            dateField.getEditor().textProperty().addListener((observable, previous, current) -> {
                if (current != null && current.trim().length() >= DatePickerSupport.DATE_PATTERN.length()) {
                    validate(false);
                }
            });
            Button remove = new Button("Xóa");
            remove.getStyleClass().add("danger-button");
            remove.setOnAction(event -> {
                revenueRows.remove(this);
                revenueRowsContainer.getChildren().remove(container);
                updateRevenueTotal();
            });
            container.getStyleClass().add("revenue-row");
            errorLabel.getStyleClass().add("field-error");
            errorLabel.setWrapText(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            fields.getChildren().addAll(referenceField, dateField, descriptionField, amountField, remove);
            container.getChildren().addAll(fields, errorLabel);
        }

        private BigDecimal amountOrZero() {
            try {
                return amountField.getText() == null || amountField.getText().isBlank()
                        ? BigDecimal.ZERO
                        : parseAmount(amountField.getText());
            } catch (NumberFormatException exception) {
                return BigDecimal.ZERO;
            }
        }

        private boolean validate() {
            return validate(true);
        }

        private boolean validate(boolean showRequiredErrors) {
            List<String> errors = new ArrayList<>();
            String reference = referenceField.getText();
            boolean referenceValid = reference == null || reference.trim().length() <= 100;
            markInvalid(referenceField, !referenceValid);
            if (!referenceValid) {
                errors.add("Số hiệu tối đa 100 ký tự");
            }

            LocalDate revenueDate = parseRevenueDate(showRequiredErrors, errors);
            BigDecimal amount = parseRevenueAmount(showRequiredErrors, errors);
            boolean valid = referenceValid && revenueDate != null && amount != null;
            setStandaloneError(errorLabel, errors.isEmpty() ? null : String.join(" • ", errors));
            return valid;
        }

        private void syncDescriptionFromSymptoms(String symptoms) {
            if (!autoFillDescription) {
                return;
            }
            syncingDescription = true;
            descriptionField.setText(nullToEmpty(symptoms));
            syncingDescription = false;
        }

        private LocalDate parseRevenueDate(boolean showRequiredErrors, List<String> errors) {
            String value = dateField.getEditor().getText();
            if (value == null || value.isBlank()) {
                markInvalid(dateField, showRequiredErrors);
                if (showRequiredErrors) {
                    errors.add("Thiếu ngày doanh thu");
                }
                return null;
            }
            try {
                LocalDate date = DatePickerSupport.commit(dateField);
                if (date.isAfter(LocalDate.now())) {
                    markInvalid(dateField, true);
                    errors.add("Ngày doanh thu không được ở tương lai");
                    return null;
                }
                dateField.setValue(date);
                markInvalid(dateField, false);
                return date;
            } catch (DateTimeParseException exception) {
                markInvalid(dateField, true);
                errors.add("Ngày doanh thu sai định dạng Ngày/Tháng/Năm");
                return null;
            }
        }

        private BigDecimal parseRevenueAmount(boolean showRequiredErrors, List<String> errors) {
            String value = amountField.getText();
            if (value == null || value.isBlank()) {
                markInvalid(amountField, showRequiredErrors);
                if (showRequiredErrors) {
                    errors.add("Thiếu số tiền");
                }
                return null;
            }
            try {
                BigDecimal amount = parseAmount(value);
                if (amount.signum() < 0) {
                    throw new IllegalArgumentException("Số tiền không được âm");
                }
                if (amount.scale() > 2 || amount.precision() - amount.scale() > 13) {
                    throw new IllegalArgumentException("Số tiền tối đa 13 chữ số và 2 số thập phân");
                }
                markInvalid(amountField, false);
                return amount;
            } catch (NumberFormatException exception) {
                markInvalid(amountField, true);
                errors.add("Số tiền không đúng định dạng");
                return null;
            } catch (IllegalArgumentException exception) {
                markInvalid(amountField, true);
                errors.add(exception.getMessage());
                return null;
            }
        }

        private Revenue toRevenue() {
            LocalDate date = DatePickerSupport.commit(dateField);
            BigDecimal amount = parseAmount(amountField.getText());
            Revenue revenue = new Revenue(
                    selectedPatient.getId(), date, emptyToNull(descriptionField.getText()), amount);
            revenue.setSoHieu(emptyToNull(referenceField.getText()));
            if (source.getId() != null) {
                revenue.setId(source.getId());
                revenue.setCreatedAt(source.getCreatedAt());
            }
            return revenue;
        }

        private BigDecimal parseAmount(String value) {
            String normalized = value.trim();
            if (!normalized.matches("-?\\d+(?:[.,]\\d+)?")) {
                throw new NumberFormatException("Unsupported amount format");
            }
            if (normalized.indexOf(',') >= 0 && normalized.indexOf('.') < 0) {
                normalized = normalized.replace(',', '.');
            }
            return new BigDecimal(normalized);
        }

        private void markInvalid(Control control, boolean invalid) {
            if (invalid && !control.getStyleClass().contains("input-invalid")) {
                control.getStyleClass().add("input-invalid");
            } else if (!invalid) {
                control.getStyleClass().remove("input-invalid");
            }
        }
    }
}
