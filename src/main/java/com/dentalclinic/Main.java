package com.dentalclinic;

import com.dentalclinic.controller.PatientController;
import com.dentalclinic.controller.ReportController;
import com.dentalclinic.config.DatabaseConfig;
import com.dentalclinic.repository.PatientRepository;
import com.dentalclinic.repository.ReportRepository;
import com.dentalclinic.repository.RepositoryTransaction;
import com.dentalclinic.repository.RevenueRepository;
import com.dentalclinic.repository.VisitRepository;
import com.dentalclinic.service.PatientService;
import com.dentalclinic.service.PdfReportService;
import com.dentalclinic.service.ReportService;
import com.dentalclinic.service.RevenueService;
import com.dentalclinic.service.VisitService;
import com.dentalclinic.service.VisitRevenueWorkflowService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static final String WINDOW_TITLE = "Quản lý hồ sơ nha khoa";

    @Override
    public void start(Stage stage) throws IOException {
        URL viewResource = requireResource("/fxml/main-view.fxml");
        DatabaseConfig databaseConfig = new DatabaseConfig();
        PatientService patientService = new PatientService(new PatientRepository(databaseConfig));
        VisitService visitService = new VisitService(new VisitRepository(databaseConfig));
        RevenueService revenueService = new RevenueService(new RevenueRepository(databaseConfig));
        VisitRevenueWorkflowService workflowService = new VisitRevenueWorkflowService(
                new RepositoryTransaction(databaseConfig)
        );
        ReportService reportService = new ReportService(new ReportRepository(databaseConfig));
        PdfReportService pdfReportService = new PdfReportService();
        FXMLLoader loader = new FXMLLoader(viewResource);
        loader.setControllerFactory(type -> createController(
                type, patientService, visitService, revenueService, workflowService,
                reportService, pdfReportService));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(requireResource("/css/style.css").toExternalForm());

        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseConfig.shutdownConnectionPool();
    }

    private static URL requireResource(String path) {
        URL resource = Main.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Required application resource was not found: " + path);
        }
        return resource;
    }

    private static Object createController(
            Class<?> type,
            PatientService patientService,
            VisitService visitService,
            RevenueService revenueService,
            VisitRevenueWorkflowService workflowService,
            ReportService reportService,
            PdfReportService pdfReportService
    ) {
        if (type == PatientController.class) {
            return new PatientController(patientService, visitService, revenueService, workflowService);
        }
        if (type == ReportController.class) {
            return new ReportController(reportService, pdfReportService);
        }
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create UI controller: " + type.getSimpleName(), exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
