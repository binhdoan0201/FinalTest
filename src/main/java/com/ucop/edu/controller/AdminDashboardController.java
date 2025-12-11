package com.ucop.edu.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AdminDashboardController {

    @FXML private Label lblWelcome;

    @FXML
    private void initialize() {
        lblWelcome.setText("CHÀO MỪNG ADMIN!");
    }

    @FXML
    private void openUserManagement() throws Exception {
        loadScene("/fxml/admin-user-list.fxml");
    }

    @FXML
    private void logout() throws Exception {
        loadScene("/fxml/login.fxml");
    }

    private void loadScene(String fxml) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) lblWelcome.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}