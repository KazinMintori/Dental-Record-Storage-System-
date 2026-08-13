package com.dentalclinic.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class ReportController {

    @FXML
    private ToggleButton patientBookOption;
    @FXML
    private ToggleButton revenueOption;
    @FXML
    private Label reportDescription;

    @FXML
    private void initialize() {
        patientBookOption.setSelected(true);
        selectPatientBook();
    }

    @FXML
    public void selectPatientBook() {
        patientBookOption.setSelected(true);
        revenueOption.setSelected(false);
        reportDescription.setText("Thông tin bệnh nhân và lịch sử các lần khám trong khoảng ngày đã chọn.");
    }

    @FXML
    public void selectRevenue() {
        patientBookOption.setSelected(false);
        revenueOption.setSelected(true);
        reportDescription.setText("Tổng hợp các khoản thu theo ngày; tổng cộng là tổng số tiền trong khoảng đã chọn.");
    }
}
