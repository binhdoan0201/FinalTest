package com.ucop.edu.controller;

import com.ucop.edu.entity.Account;
import com.ucop.edu.util.CurrentUser;
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
        Account acc = CurrentUser.getCurrentAccount();
        String name = "ADMIN";

        if (acc != null) {
            if (acc.getProfile() != null && acc.getProfile().getFullName() != null && !acc.getProfile().getFullName().trim().isEmpty()) {
                name = acc.getProfile().getFullName();
            } else {
                name = acc.getUsername(); // dùng username nếu chưa có họ tên
            }
        }

        lblWelcome.setText("CHÀO MỪNG " + name.toUpperCase() + "!");
    }

    @FXML
    private void openUserManagement() throws Exception {
        loadScene("/fxml/admin-user-list.fxml");
    }
    
    @FXML
    private void openCourseManagement() throws Exception {
    	Parent root = FXMLLoader.load(getClass().getResource("/fxml/category_course_view.fxml"));
        Stage stage = (Stage) lblWelcome.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("UCOP - Universal Commerce & Operations Platform");
        stage.centerOnScreen();
        stage.setMaximized(true); 
    }
    
    @FXML
    private void logout() throws Exception {
        CurrentUser.clear();
        loadScene("/fxml/login.fxml");
    }

    private void loadScene(String fxml) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) lblWelcome.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("UCOP - Universal Commerce & Operations Platform");
        stage.centerOnScreen();
    }
}