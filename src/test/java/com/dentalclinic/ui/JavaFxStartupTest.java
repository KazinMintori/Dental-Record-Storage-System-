package com.dentalclinic.ui;

import com.dentalclinic.Main;
import com.dentalclinic.controller.PatientController;
import com.dentalclinic.model.Patient;
import com.dentalclinic.model.PatientPage;
import com.dentalclinic.model.PatientSearchCriteria;
import com.dentalclinic.model.Visit;
import com.dentalclinic.repository.PatientRepository;
import com.dentalclinic.repository.RepositoryException;
import com.dentalclinic.repository.RepositoryTransaction;
import com.dentalclinic.repository.RevenueRepository;
import com.dentalclinic.repository.VisitRepository;
import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.service.PatientService;
import com.dentalclinic.service.RevenueService;
import com.dentalclinic.service.VisitService;
import com.dentalclinic.service.VisitRevenueWorkflowService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxStartupTest {

    @Test
    void mainFxmlLoadsAndPatientUiUsesServices() throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> {
            try {
                verifyPatientUiIntegration();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        assertTrue(completed.await(10, TimeUnit.SECONDS), "JavaFX verification did not finish in time");
        assertNull(failure.get(), () -> "JavaFX UI verification failed: " + failure.get());
        Platform.exit();
    }

    private static void verifyPatientUiIntegration() throws Exception {
        Patient first = patient(1L, "Nguyễn An", null);
        Patient second = patient(2L, "Trần Bình", "Huế");
        FakePatientRepository patientRepository = new FakePatientRepository(first, second);
        FakeVisitRepository visitRepository = new FakeVisitRepository(
                visit(10L, 1L, 1, "Đau răng"),
                visit(11L, 1L, 2, "Ê buốt")
        );
        PatientController injectedController = new PatientController(
                new PatientService(patientRepository),
                new VisitService(visitRepository),
                new RevenueService(new FakeRevenueRepository()),
                new FakeWorkflowService(visitRepository),
                Runnable::run,
                Duration.ZERO
        );

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/main-view.fxml"));
        loader.setControllerFactory(type -> type == PatientController.class
                ? injectedController
                : construct(type));
        Parent root = loader.load();

        assertNotNull(root);
        assertNotNull(Main.class.getResource("/css/style.css"));
        assertEquals(2, injectedController.getDisplayedPatientCount());
        assertNull(injectedController.getSelectedPatient());
        TableView<Patient> table = field(injectedController, "patientTable", TableView.class);
        assertEquals("Hiển thị 1 - 2 trên tổng số 2 bệnh nhân",
                field(injectedController, "paginationStatusLabel", Label.class).getText());
        assertEquals("Trang 1 / 1", field(injectedController, "pageNumberLabel", Label.class).getText());
        assertTrue(field(injectedController, "nextPageButton", Button.class).isDisable());
        Button bulkTrash = field(injectedController, "bulkTrashButton", Button.class);
        CheckBox selectAll = field(injectedController, "selectAllPatientsCheckBox", CheckBox.class);
        assertTrue(bulkTrash.isDisable());
        selectAll.fire();
        assertFalse(bulkTrash.isDisable());
        assertEquals("Thùng rác (2)", bulkTrash.getText());
        selectAll.fire();
        assertTrue(bulkTrash.isDisable());
        selectPatient(injectedController, table.getItems().getFirst());
        assertSame(first, injectedController.getSelectedPatient());
        assertEquals(2, injectedController.getDisplayedVisitCount());
        assertEquals("—", field(injectedController, "patientAddressValue", Label.class).getText());

        TextField search = field(injectedController, "searchField", TextField.class);
        search.setText("bình");
        injectedController.searchPatients();
        assertEquals("bình", patientRepository.lastSearch);
        assertEquals(1, injectedController.getDisplayedPatientCount());
        selectPatient(injectedController, table.getItems().getFirst());
        assertSame(second, injectedController.getSelectedPatient());
        assertEquals(0, injectedController.getSelectedPatientVisitCount(),
                "A patient must never inherit visits from the previously opened profile.");

        search.clear();
        injectedController.searchPatients();
        assertEquals(2, injectedController.getDisplayedPatientCount());
        assertTrue(patientRepository.findAllCalls >= 2);

        search.setText("Nguyễn");
        field(injectedController, "genderFilterField", ComboBox.class).setValue("Nam");
        injectedController.searchPatients();
        assertEquals(1, injectedController.getDisplayedPatientCount());
        assertEquals("Nam", patientRepository.lastCriteria.gender());

        search.setText("xyz123999");
        field(injectedController, "genderFilterField", ComboBox.class).setValue(null);
        injectedController.searchPatients();
        assertEquals(0, injectedController.getDisplayedPatientCount());
        assertEquals("Không tìm thấy kết quả",
                field(injectedController, "patientTablePlaceholder", Label.class).getText());

        injectedController.clearAllSearchFilters();
        assertEquals(2, injectedController.getDisplayedPatientCount());
        assertEquals("", search.getText());
        assertNull(field(injectedController, "genderFilterField", ComboBox.class).getValue());

        injectedController.openEditPatientForm();
        field(injectedController, "patientNameField", TextField.class).setText("Nguyễn An Updated");
        injectedController.savePatient();
        assertEquals("Nguyễn An Updated", patientRepository.updatedPatient.getHoVaTen());

        injectedController.openNewPatientForm();
        field(injectedController, "patientNameField", TextField.class).setText("Đỗ Minh Đức");
        field(injectedController, "patientPhoneField", TextField.class).setText("0123456789");
        field(injectedController, "patientGenderField", ComboBox.class).setValue("Nữ");
        field(injectedController, "patientBirthDateField", DatePicker.class).setValue(LocalDate.of(2000, 3, 4));
        assertEquals("04/03/2000", field(injectedController, "patientBirthDateField", DatePicker.class)
                .getConverter().toString(LocalDate.of(2000, 3, 4)));
        injectedController.savePatient();
        assertEquals("Đỗ Minh Đức", patientRepository.createdPatient.getHoVaTen());
        assertEquals("0123456789", patientRepository.createdPatient.getSoDienThoai());
        assertEquals("Đỗ Minh Đức", field(injectedController, "patientNameValue", Label.class).getText());
        assertTrue(table.getItems().stream().anyMatch(patient -> "Đỗ Minh Đức".equals(patient.getHoVaTen())));
        assertEquals(0, injectedController.getSelectedPatientVisitCount(),
                "A newly created patient must start with an independent empty visit history.");

        Patient selected = injectedController.getSelectedPatient();
        injectedController.openVisitForm();
        field(injectedController, "visitDateField", DatePicker.class).setValue(LocalDate.of(2026, 8, 13));
        field(injectedController, "dentistField", TextField.class).setText("BS. Minh");
        field(injectedController, "symptomsField", TextArea.class).setText("Đau");
        field(injectedController, "diagnosisField", TextArea.class).setText("Sâu răng");
        field(injectedController, "treatmentField", TextArea.class).setText("Trám răng");
        injectedController.addRevenueRow();
        VBox revenueRows = field(injectedController, "revenueRowsContainer", VBox.class);
        VBox firstRevenue = (VBox) revenueRows.getChildren().getFirst();
        HBox firstRevenueFields = (HBox) firstRevenue.getChildren().getFirst();
        TextField firstDescription = (TextField) firstRevenueFields.getChildren().get(2);
        TextField firstAmount = (TextField) firstRevenueFields.getChildren().get(3);
        assertEquals("Đau", firstDescription.getText());
        firstDescription.setText("Mô tả tự sửa");
        firstAmount.setText("100.50");
        field(injectedController, "symptomsField", TextArea.class).setText("Đau tăng");
        assertEquals("Mô tả tự sửa", firstDescription.getText());
        injectedController.addRevenueRow();
        VBox secondRevenue = (VBox) revenueRows.getChildren().get(1);
        HBox secondRevenueFields = (HBox) secondRevenue.getChildren().getFirst();
        assertEquals("Mô tả tự sửa", firstDescription.getText());
        TextField secondDescription = (TextField) secondRevenueFields.getChildren().get(2);
        assertEquals("Đau tăng", secondDescription.getText());
        field(injectedController, "symptomsField", TextArea.class).setText("Ê buốt");
        assertEquals("Mô tả tự sửa", firstDescription.getText());
        assertEquals("Ê buốt", secondDescription.getText());
        ((TextField) secondRevenueFields.getChildren().get(3)).setText("200.25");
        assertEquals(new java.math.BigDecimal("300.75"), injectedController.getRevenueTotal());
        ((Button) secondRevenueFields.getChildren().get(4)).fire();
        assertEquals(1, injectedController.getRevenueRowCount());
        assertEquals(new java.math.BigDecimal("100.50"), injectedController.getRevenueTotal());
        injectedController.saveVisit();
        assertNotNull(visitRepository.createdVisit, () -> "Visit was not saved. Status: "
                + fieldUnchecked(injectedController, "statusMessage", Label.class).getText()
                + "; date: " + fieldUnchecked(injectedController, "visitDateError", Label.class).getText()
                + "; revenue: " + fieldUnchecked(injectedController, "visitRevenueError", Label.class).getText());
        assertEquals(selected.getId(), visitRepository.createdVisit.getPatientId());

        injectedController.openVisitForm();
        DatePicker invalidVisitDate = field(injectedController, "visitDateField", DatePicker.class);
        invalidVisitDate.getEditor().setText("31/02/2026");
        injectedController.saveVisit();
        assertEquals("Ngày không hợp lệ. Hãy nhập theo Ngày/Tháng/Năm.",
                field(injectedController, "visitDateError", Label.class).getText());
        assertTrue(field(injectedController, "dentistError", Label.class).isVisible());
        assertTrue(field(injectedController, "symptomsError", Label.class).isVisible());
        assertTrue(invalidVisitDate.getStyleClass().contains("input-invalid"));
        injectedController.cancelVisitForm();

        injectedController.openVisitForm();
        field(injectedController, "dentistField", TextField.class).setText("BS. Minh");
        field(injectedController, "symptomsField", TextArea.class).setText("Đau răng");
        field(injectedController, "diagnosisField", TextArea.class).setText("Sâu răng");
        field(injectedController, "treatmentField", TextArea.class).setText("Trám răng");
        injectedController.addRevenueRow();
        VBox invalidRevenue = (VBox) revenueRows.getChildren().getFirst();
        HBox invalidRevenueFields = (HBox) invalidRevenue.getChildren().getFirst();
        TextField invalidAmount = (TextField) invalidRevenueFields.getChildren().get(3);
        invalidAmount.setText("12abc");
        injectedController.saveVisit();
        assertTrue(invalidAmount.getStyleClass().contains("input-invalid"));
        assertTrue(((Label) invalidRevenue.getChildren().get(1)).getText()
                .contains("Số tiền không đúng định dạng"));
        injectedController.cancelVisitForm();

        injectedController.openNewPatientForm();
        field(injectedController, "patientNameField", TextField.class).setText("  ");
        field(injectedController, "patientGenderField", ComboBox.class).setValue("Nam");
        field(injectedController, "patientBirthDateField", DatePicker.class).setValue(LocalDate.now());
        injectedController.savePatient();
        Label status = field(injectedController, "statusMessage", Label.class);
        assertEquals("Vui lòng nhập đầy đủ thông tin bắt buộc.", status.getText());

        patientRepository.delete(first.getId());
        injectedController.openTrash();
        assertEquals(1, injectedController.getDisplayedPatientCount());
        assertTrue(field(injectedController, "patientSelectColumn", javafx.scene.control.TableColumn.class).isVisible());
        assertFalse(selectAll.isDisable());
        selectAll.fire();
        assertTrue(field(injectedController, "trashActions", HBox.class).isVisible());
        assertEquals("Đã chọn 1 hồ sơ",
                field(injectedController, "trashSelectedCountLabel", Label.class).getText());
        field(injectedController, "bulkRestoreButton", Button.class).fire();
        assertEquals(0, injectedController.getDisplayedPatientCount());
        injectedController.closeTrash();
        assertEquals(3, injectedController.getDisplayedPatientCount());

        patientRepository.failLoading = true;
        injectedController.refreshPatients();
        assertEquals("Không thể tải danh sách bệnh nhân.", status.getText());
        assertFalse(status.getText().contains("jdbc:"));
        assertFalse(status.getText().contains("secret-password"));
    }

    private static Object construct(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Patient patient(Long id, String name, String address) {
        return new Patient(id, name, "Nam", LocalDate.of(1990, 1, 2),
                null, null, address, null, null, null, null);
    }

    private static Visit visit(Long id, Long patientId, int sequence, String symptoms) {
        return new Visit(id, patientId, sequence, LocalDate.of(2026, 8, 1), symptoms,
                "Chẩn đoán", "Điều trị", "BS. An", null, null, null);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static <T> T fieldUnchecked(Object target, String name, Class<T> type) {
        try {
            return field(target, name, type);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static void selectPatient(PatientController controller, Patient patient) throws Exception {
        Method method = PatientController.class.getDeclaredMethod("selectPatient", Patient.class);
        method.setAccessible(true);
        method.invoke(controller, patient);
    }

    private static final class FakePatientRepository extends PatientRepository {
        private final List<Patient> patients = new ArrayList<>();
        private final List<Patient> deletedPatients = new ArrayList<>();
        private int findAllCalls;
        private String lastSearch;
        private PatientSearchCriteria lastCriteria;
        private Patient createdPatient;
        private Patient updatedPatient;
        private boolean failLoading;

        private FakePatientRepository(Patient... patients) {
            super(() -> null);
            this.patients.addAll(List.of(patients));
        }

        @Override
        public List<Patient> findAll() {
            if (failLoading) {
                throw new RepositoryException("jdbc:postgresql://host/db?password=secret-password");
            }
            findAllCalls++;
            return List.copyOf(patients);
        }

        @Override
        public List<Patient> findByName(String name) {
            lastSearch = name;
            String query = name.toLowerCase();
            return patients.stream()
                    .filter(patient -> patient.getHoVaTen().toLowerCase().contains(query))
                    .toList();
        }

        @Override
        public List<Patient> search(PatientSearchCriteria criteria) {
            lastCriteria = criteria;
            lastSearch = criteria.name();
            if (criteria.isEmpty()) {
                return findAll();
            }
            List<String> tokens = criteria.nameTokens().stream().map(JavaFxStartupTest::searchText).toList();
            return patients.stream()
                    .filter(patient -> tokens.stream().allMatch(searchText(patient.getHoVaTen())::contains))
                    .filter(patient -> criteria.patientCode() == null
                            || patient.getId().toString().equals(criteria.patientCode())
                            || ("BN-%06d".formatted(patient.getId())).equalsIgnoreCase(criteria.patientCode()))
                    .filter(patient -> criteria.phone() == null
                            || patient.getSoDienThoai() != null && patient.getSoDienThoai().contains(criteria.phone()))
                    .filter(patient -> criteria.birthDate() == null
                            || criteria.birthDate().equals(patient.getNgaySinh()))
                    .filter(patient -> criteria.gender() == null
                            || searchText(criteria.gender()).equals(searchText(patient.getGioiTinh())))
                    .toList();
        }

        @Override
        public PatientPage searchPage(PatientSearchCriteria criteria, int pageIndex, int pageSize) {
            List<Patient> matches = search(criteria);
            int effectivePage = matches.isEmpty() ? 0
                    : Math.min(pageIndex, (matches.size() - 1) / pageSize);
            int from = Math.min(effectivePage * pageSize, matches.size());
            int to = Math.min(from + pageSize, matches.size());
            return new PatientPage(matches.subList(from, to), matches.size(), effectivePage, pageSize);
        }

        @Override
        public Patient save(Patient patient) {
            patient.setId(100L + patients.size());
            createdPatient = patient;
            patients.add(patient);
            return patient;
        }

        @Override
        public void update(Patient patient) {
            updatedPatient = patient;
            patients.replaceAll(existing -> existing.getId().equals(patient.getId()) ? patient : existing);
        }

        @Override
        public Patient findById(Long id) {
            return patients.stream().filter(patient -> patient.getId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public List<Patient> findDeleted() {
            return List.copyOf(deletedPatients);
        }

        @Override
        public PatientPage findDeletedPage(int pageIndex, int pageSize) {
            int effectivePage = deletedPatients.isEmpty() ? 0
                    : Math.min(pageIndex, (deletedPatients.size() - 1) / pageSize);
            int from = Math.min(effectivePage * pageSize, deletedPatients.size());
            int to = Math.min(from + pageSize, deletedPatients.size());
            return new PatientPage(deletedPatients.subList(from, to), deletedPatients.size(), effectivePage, pageSize);
        }

        @Override
        public void deleteAll(List<Long> ids) {
            ids.forEach(this::delete);
        }

        @Override
        public void delete(Long id) {
            Patient patient = patients.stream()
                    .filter(candidate -> candidate.getId().equals(id)).findFirst().orElse(null);
            if (patient != null) {
                patients.remove(patient);
                patient.setDeletedAt(java.time.OffsetDateTime.now());
                deletedPatients.add(patient);
            }
        }

        @Override
        public void restore(Long id) {
            Patient patient = deletedPatients.stream()
                    .filter(candidate -> candidate.getId().equals(id)).findFirst().orElse(null);
            if (patient != null) {
                deletedPatients.remove(patient);
                patient.setDeletedAt(null);
                patients.add(patient);
            }
        }

        @Override
        public void restoreAll(List<Long> ids) {
            ids.forEach(this::restore);
        }

        @Override
        public void permanentlyDelete(Long id) {
            deletedPatients.removeIf(patient -> patient.getId().equals(id));
        }

        @Override
        public void permanentlyDeleteAll(List<Long> ids) {
            ids.forEach(this::permanentlyDelete);
        }
    }

    private static final class FakeVisitRepository extends VisitRepository {
        private final List<Visit> visits = new ArrayList<>();
        private Visit createdVisit;

        private FakeVisitRepository(Visit... visits) {
            super(() -> null);
            this.visits.addAll(List.of(visits));
        }

        @Override
        public List<Visit> findByPatientId(Long patientId) {
            return visits.stream().filter(visit -> visit.getPatientId().equals(patientId)).toList();
        }

        @Override
        public Visit save(Visit visit) {
            visit.setId(100L + visits.size());
            createdVisit = visit;
            visits.add(visit);
            return visit;
        }
    }

    private static final class FakeRevenueRepository extends RevenueRepository {
        private FakeRevenueRepository() {
            super(() -> null);
        }

        @Override
        public List<com.dentalclinic.model.Revenue> findByVisitId(Long visitId) {
            return List.of();
        }
    }

    private static final class FakeWorkflowService extends VisitRevenueWorkflowService {
        private final FakeVisitRepository visitRepository;

        private FakeWorkflowService(FakeVisitRepository visitRepository) {
            super(new RepositoryTransaction(() -> {
                throw new SQLException("The fake workflow does not open a database connection.");
            }));
            this.visitRepository = visitRepository;
        }

        @Override
        public VisitRevenueRecord create(Visit visit, List<com.dentalclinic.model.Revenue> revenues) {
            return new VisitRevenueRecord(visitRepository.save(visit), revenues);
        }

        @Override
        public VisitRevenueRecord update(Visit visit, List<com.dentalclinic.model.Revenue> revenues) {
            visitRepository.createdVisit = visit;
            return new VisitRevenueRecord(visit, revenues);
        }
    }

    private static String searchText(String value) {
        return Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }
}
