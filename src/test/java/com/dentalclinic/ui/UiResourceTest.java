package com.dentalclinic.ui;

import com.dentalclinic.Main;
import com.dentalclinic.controller.MainController;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiResourceTest {

    @Test
    void applicationResourcesExist() {
        assertNotNull(Main.class.getResource("/fxml/main-view.fxml"));
        assertNotNull(Main.class.getResource("/fxml/patient-screen.fxml"));
        assertNotNull(Main.class.getResource("/fxml/report-screen.fxml"));
        assertNotNull(Main.class.getResource("/css/style.css"));
        assertNotNull(Main.class.getResource("/db/migration/V3__patient_trash_and_gender.sql"));
        assertNotNull(Main.class.getResource("/db/migration/V4__patient_pagination_indexes_and_optional_revenue.sql"));
    }

    @Test
    void allFxmlResourcesAreWellFormedXml() throws Exception {
        parseFxml("main-view.fxml");
        parseFxml("patient-screen.fxml");
        parseFxml("report-screen.fxml");
    }

    @Test
    void mainShellContainsBothNavigationDestinations() throws Exception {
        String fxml = resourceText("/fxml/main-view.fxml");
        assertTrue(fxml.contains("text=\"Bệnh nhân\""));
        assertTrue(fxml.contains("text=\"In báo cáo\""));
        assertTrue(fxml.contains("patient-screen.fxml"));
        assertTrue(fxml.contains("report-screen.fxml"));
    }

    @Test
    void navigationDestinationsAreDistinct() {
        assertEquals(2, MainController.Destination.values().length);
        assertEquals(MainController.Destination.PATIENTS, MainController.Destination.valueOf("PATIENTS"));
        assertEquals(MainController.Destination.REPORTS, MainController.Destination.valueOf("REPORTS"));
    }

    @Test
    void patientScreenProvidesDynamicListsDetailsAndForms() throws Exception {
        String fxml = resourceText("/fxml/patient-screen.fxml");
        assertTrue(fxml.contains("fx:id=\"patientTable\""));
        assertTrue(fxml.contains("<TableView fx:id=\"patientTable\""));
        assertTrue(fxml.contains("fx:id=\"patientCodeColumn\""));
        assertTrue(fxml.contains("fx:id=\"patientNameColumn\""));
        assertTrue(fxml.contains("fx:id=\"patientBirthDateColumn\""));
        assertTrue(fxml.contains("fx:id=\"patientPhoneColumn\""));
        assertTrue(fxml.contains("fx:id=\"patientSelectColumn\""));
        assertTrue(fxml.contains("fx:id=\"patientActionColumn\""));
        assertTrue(fxml.contains("fx:id=\"tableLoadingOverlay\""));
        assertTrue(fxml.contains("fx:id=\"paginationStatusLabel\""));
        assertTrue(fxml.contains("onAction=\"#goToFirstPage\""));
        assertTrue(fxml.contains("onAction=\"#goToLastPage\""));
        assertTrue(fxml.contains("fx:id=\"advancedFilterPanel\""));
        assertTrue(fxml.contains("fx:id=\"patientCodeFilterField\""));
        assertTrue(fxml.contains("fx:id=\"phoneFilterField\""));
        assertTrue(fxml.contains("fx:id=\"birthDateFilterField\""));
        assertTrue(fxml.contains("fx:id=\"genderFilterField\""));
        assertTrue(fxml.contains("onAction=\"#clearAllSearchFilters\""));
        assertTrue(fxml.contains("fx:id=\"patientLoadingView\""));
        assertTrue(fxml.contains("onAction=\"#openTrash\""));
        assertTrue(fxml.contains("text=\"Xem thùng rác\" onAction=\"#openTrash\""));
        assertTrue(fxml.contains("text=\"Thùng rác\" onAction=\"#moveSelectedPatientsToTrash\""));
        assertTrue(fxml.contains("text=\"Tìm\" onAction=\"#searchPatients\" styleClass=\"utility-button\""));
        assertTrue(fxml.contains("text=\"Bộ lọc\" onAction=\"#toggleAdvancedFilters\" styleClass=\"utility-button\""));
        assertTrue(fxml.contains("fx:id=\"bulkRestoreButton\""));
        assertTrue(fxml.contains("onAction=\"#restoreSelectedPatients\""));
        assertTrue(fxml.contains("fx:id=\"bulkPermanentDeleteButton\""));
        assertTrue(fxml.contains("onAction=\"#permanentlyDeleteSelectedPatients\""));
        assertTrue(fxml.contains("<ComboBox fx:id=\"patientGenderField\""));
        assertTrue(fxml.contains("Ngày/Tháng/Năm"));
        assertTrue(fxml.contains("Không tìm thấy kết quả") || fxml.contains("fx:id=\"patientTablePlaceholder\""));
        assertTrue(fxml.contains("fx:id=\"directoryView\""));
        assertTrue(fxml.contains("fx:id=\"detailView\""));
        assertTrue(fxml.contains("fx:id=\"visitListContainer\""));
        assertTrue(fxml.contains("onAction=\"#showDirectory\""));
        assertTrue(fxml.contains("onAction=\"#searchPatients\""));
        assertTrue(fxml.contains("onAction=\"#savePatient\""));
        assertTrue(fxml.contains("onAction=\"#saveVisit\""));
        assertTrue(fxml.contains("fx:id=\"revenueRowsContainer\""));
        assertTrue(fxml.contains("fx:id=\"revenueTotalLabel\""));
        assertTrue(fxml.contains("onAction=\"#addRevenueRow\""));
        assertTrue(fxml.contains("onAction=\"#deleteCurrentVisit\""));
        assertTrue(fxml.contains("fx:id=\"visitDateError\""));
        assertTrue(fxml.contains("fx:id=\"dentistError\""));
        assertTrue(fxml.contains("fx:id=\"symptomsError\""));
        assertTrue(fxml.contains("fx:id=\"diagnosisError\""));
        assertTrue(fxml.contains("fx:id=\"treatmentError\""));
        assertTrue(fxml.contains("fx:id=\"visitRevenueError\""));
        assertFalse(fxml.contains("Nguyễn Minh Anh"));
        assertFalse(fxml.contains("Trần Thị Mai"));
    }

    @Test
    void reportScreenContainsTwoTypesAndOnlyCustomDateRange() throws Exception {
        String fxml = resourceText("/fxml/report-screen.fxml");
        assertTrue(fxml.contains("Sổ khám bệnh"));
        assertTrue(fxml.contains("Tổng doanh thu"));
        assertTrue(fxml.contains("fx:id=\"fromDatePicker\""));
        assertTrue(fxml.contains("fx:id=\"toDatePicker\""));
        assertEquals(2, occurrences(fxml, "<DatePicker"));
        assertTrue(fxml.contains("Xem trước"));
        assertTrue(fxml.contains("text=\"In / Xuất PDF\""));
        assertTrue(fxml.contains("onAction=\"#previewReport\""));
        assertTrue(fxml.contains("onAction=\"#exportPdf\""));
        assertTrue(fxml.contains("fx:id=\"reportProgress\""));
    }

    @Test
    void cssDefinesReusableVisualStates() throws Exception {
        String css = resourceText("/css/style.css");
        assertTrue(css.contains(".navigation-button-active"));
        assertTrue(css.contains(".patient-card-selected"));
        assertTrue(css.contains(".visit-card"));
        assertTrue(css.contains(".report-option:selected"));
        assertTrue(css.contains(".report-preview-table"));
        assertTrue(css.contains(".report-preview-frozen-table"));
        assertTrue(css.contains(".revenue-paper-page"));
        assertTrue(css.contains(".revenue-paper-grid"));
        assertTrue(css.contains(".revenue-paper-data-cell"));
        assertTrue(css.contains(".revenue-paper-meta-italic"));
        assertTrue(css.contains(".input-invalid"));
        assertTrue(css.contains(".field-error"));
        assertTrue(css.contains(".utility-button:hover"));
        assertTrue(css.contains(".primary-button:hover"));
        assertTrue(css.contains(".danger-button:hover"));
        assertTrue(css.contains(".pagination-button:hover"));
        assertTrue(css.contains(".table-row-cell:odd"));
        assertTrue(css.contains(".table-action-edit"));
        assertTrue(css.contains(".table-cell.checkbox-column"));
        assertTrue(css.contains("-fx-alignment: CENTER"));
        assertTrue(css.contains(".restore-button:hover"));
        assertTrue(css.contains("-fx-border-color: #2563eb"));
    }

    @Test
    void trashMigrationConstrainsGenderAndKeepsDeletionRecoverable() throws Exception {
        String migration = resourceText("/db/migration/V3__patient_trash_and_gender.sql");
        assertTrue(migration.contains("deleted_at TIMESTAMP WITH TIME ZONE"));
        assertTrue(migration.contains("CHECK (gioi_tinh IN ('Nam', 'Nữ', 'Khác'))"));
        assertTrue(migration.contains("idx_patients_active_search"));
        assertTrue(migration.contains("idx_patients_deleted_at"));
    }

    @Test
    void paginationMigrationAddsIndexesAndOptionalRevenueDescription() throws Exception {
        String migration = resourceText("/db/migration/V4__patient_pagination_indexes_and_optional_revenue.sql");
        assertTrue(migration.contains("ALTER COLUMN dien_giai DROP NOT NULL"));
        assertTrue(migration.contains("idx_patients_active_search"));
        assertTrue(migration.contains("idx_patients_active_phone_search"));
        assertTrue(migration.contains("idx_patients_active_id"));
    }

    private static Document parseFxml(String fileName) throws Exception {
        try (InputStream input = Main.class.getResourceAsStream("/fxml/" + fileName)) {
            assertNotNull(input);
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        }
    }

    private static String resourceText(String path) throws Exception {
        try (InputStream input = Main.class.getResourceAsStream(path)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int occurrences(String text, String target) {
        return (text.length() - text.replace(target, "").length()) / target.length();
    }
}
