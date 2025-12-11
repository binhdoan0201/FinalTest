package com.ucop.edu.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() throws Exception {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if ("admin".equals(user) && "123".equals(pass)) {
            loadScene("/fxml/admin-dashboard.fxml");
        } else {
            errorLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    private void loadScene(String fxml) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}