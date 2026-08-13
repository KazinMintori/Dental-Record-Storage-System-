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
        assertTrue(fxml.contains("fx:id=\"patientListContainer\""));
        assertTrue(fxml.contains("<TilePane fx:id=\"patientListContainer\""));
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
        assertTrue(fxml.contains("text=\"In\""));
    }

    @Test
    void cssDefinesReusableVisualStates() throws Exception {
        String css = resourceText("/css/style.css");
        assertTrue(css.contains(".navigation-button-active"));
        assertTrue(css.contains(".patient-card-selected"));
        assertTrue(css.contains(".visit-card"));
        assertTrue(css.contains(".report-option:selected"));
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
