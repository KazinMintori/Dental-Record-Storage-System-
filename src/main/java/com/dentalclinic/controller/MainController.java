package com.dentalclinic.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class MainController {

    public enum Destination {
        PATIENTS,
        REPORTS
    }

    @FXML
    private Button patientsNavigationButton;
    @FXML
    private Button reportsNavigationButton;
    @FXML
    private Node patientScreen;
    @FXML
    private Node reportScreen;

    private Destination selectedDestination = Destination.PATIENTS;

    @FXML
    private void initialize() {
        showPatients();
    }

    @FXML
    public void showPatients() {
        select(Destination.PATIENTS);
    }

    @FXML
    public void showReports() {
        select(Destination.REPORTS);
    }

    public Destination getSelectedDestination() {
        return selectedDestination;
    }

    private void select(Destination destination) {
        selectedDestination = destination;
        boolean showPatients = destination == Destination.PATIENTS;
        setDisplayed(patientScreen, showPatients);
        setDisplayed(reportScreen, !showPatients);
        patientsNavigationButton.getStyleClass().remove("navigation-button-active");
        reportsNavigationButton.getStyleClass().remove("navigation-button-active");
        (showPatients ? patientsNavigationButton : reportsNavigationButton)
                .getStyleClass().add("navigation-button-active");
    }

    private static void setDisplayed(Node node, boolean displayed) {
        node.setVisible(displayed);
        node.setManaged(displayed);
    }
}
