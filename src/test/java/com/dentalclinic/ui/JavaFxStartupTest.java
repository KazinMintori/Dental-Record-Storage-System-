package com.dentalclinic.ui;

import com.dentalclinic.Main;
import com.dentalclinic.controller.PatientController;
import com.dentalclinic.model.Patient;
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
import javafx.scene.layout.TilePane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
                Runnable::run
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
        TilePane cards = field(injectedController, "patientListContainer", TilePane.class);
        cards.getChildren().getFirst().getOnMouseClicked().handle(null);
        assertSame(first, injectedController.getSelectedPatient());
        assertEquals(2, injectedController.getDisplayedVisitCount());
        assertEquals("—", field(injectedController, "patientAddressValue", Label.class).getText());

        TextField search = field(injectedController, "searchField", TextField.class);
        search.setText("bình");
        injectedController.searchPatients();
        assertEquals("bình", patientRepository.lastSearch);
        assertEquals(1, injectedController.getDisplayedPatientCount());
        cards.getChildren().getFirst().getOnMouseClicked().handle(null);
        assertSame(second, injectedController.getSelectedPatient());

        search.clear();
        injectedController.searchPatients();
        assertEquals(2, injectedController.getDisplayedPatientCount());
        assertTrue(patientRepository.findAllCalls >= 2);

        injectedController.openEditPatientForm();
        field(injectedController, "patientNameField", TextField.class).setText("Nguyễn An Updated");
        injectedController.savePatient();
        assertEquals("Nguyễn An Updated", patientRepository.updatedPatient.getHoVaTen());

        injectedController.openNewPatientForm();
        field(injectedController, "patientNameField", TextField.class).setText("Lê Chi");
        field(injectedController, "patientGenderField", TextField.class).setText("Nữ");
        field(injectedController, "patientBirthDateField", DatePicker.class).setValue(LocalDate.of(2000, 3, 4));
        injectedController.savePatient();
        assertEquals("Lê Chi", patientRepository.createdPatient.getHoVaTen());

        Patient selected = injectedController.getSelectedPatient();
        injectedController.openVisitForm();
        field(injectedController, "visitDateField", DatePicker.class).setValue(LocalDate.of(2026, 8, 13));
        field(injectedController, "dentistField", TextField.class).setText("BS. Minh");
        field(injectedController, "symptomsField", TextArea.class).setText("Đau");
        field(injectedController, "diagnosisField", TextArea.class).setText("Sâu răng");
        field(injectedController, "treatmentField", TextArea.class).setText("Trám răng");
        injectedController.addRevenueRow();
        VBox revenueRows = field(injectedController, "revenueRowsContainer", VBox.class);
        HBox firstRevenue = (HBox) revenueRows.getChildren().getFirst();
        TextField firstDescription = (TextField) firstRevenue.getChildren().get(2);
        TextField firstAmount = (TextField) firstRevenue.getChildren().get(3);
        assertEquals("Trám răng", firstDescription.getText());
        firstDescription.setText("Mô tả tự sửa");
        firstAmount.setText("100.50");
        field(injectedController, "treatmentField", TextArea.class).setText("Điều trị mới");
        injectedController.addRevenueRow();
        HBox secondRevenue = (HBox) revenueRows.getChildren().get(1);
        assertEquals("Mô tả tự sửa", firstDescription.getText());
        assertEquals("Điều trị mới", ((TextField) secondRevenue.getChildren().get(2)).getText());
        ((TextField) secondRevenue.getChildren().get(3)).setText("200.25");
        assertEquals(new java.math.BigDecimal("300.75"), injectedController.getRevenueTotal());
        ((Button) secondRevenue.getChildren().get(4)).fire();
        assertEquals(1, injectedController.getRevenueRowCount());
        assertEquals(new java.math.BigDecimal("100.50"), injectedController.getRevenueTotal());
        injectedController.saveVisit();
        assertEquals(selected.getId(), visitRepository.createdVisit.getPatientId());

        injectedController.openNewPatientForm();
        field(injectedController, "patientNameField", TextField.class).setText("  ");
        field(injectedController, "patientGenderField", TextField.class).setText("Nam");
        field(injectedController, "patientBirthDateField", DatePicker.class).setValue(LocalDate.now());
        injectedController.savePatient();
        Label status = field(injectedController, "statusMessage", Label.class);
        assertEquals("Vui lòng nhập đầy đủ thông tin bắt buộc.", status.getText());

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

    private static final class FakePatientRepository extends PatientRepository {
        private final List<Patient> patients = new ArrayList<>();
        private int findAllCalls;
        private String lastSearch;
        private Patient createdPatient;
        private Patient updatedPatient;
        private boolean failLoading;

        private FakePatientRepository(Patient... patients) {
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
    }

    private static final class FakeVisitRepository extends VisitRepository {
        private final List<Visit> visits = new ArrayList<>();
        private Visit createdVisit;

        private FakeVisitRepository(Visit... visits) {
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
        @Override
        public List<com.dentalclinic.model.Revenue> findByVisitId(Long visitId) {
            return List.of();
        }
    }

    private static final class FakeWorkflowService extends VisitRevenueWorkflowService {
        private final FakeVisitRepository visitRepository;

        private FakeWorkflowService(FakeVisitRepository visitRepository) {
            super(new RepositoryTransaction(new DatabaseConfig()));
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
}
