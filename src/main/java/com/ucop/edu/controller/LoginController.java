package com.ucop.edu.controller;

import com.ucop.edu.entity.Account;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.hibernate.Session;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Account account = session.createQuery(
                    "FROM Account WHERE username = :u", Account.class)
                    .setParameter("u", username)
                    .uniqueResult();

            if (account == null || !account.isEnabled()) {
                errorLabel.setText("Tài khoản không tồn tại hoặc bị khóa!");
                return;
            }

            // Tạm thời so sánh plain text để test
            if (!password.equals(account.getPassword())) {
                errorLabel.setText("Sai mật khẩu!");
                return;
            }

            // Login OK
            CurrentUser.setCurrentAccount(account);
            errorLabel.setText("");

            String role = account.getRole() == null ? "" : account.getRole().toUpperCase();

            switch (role) {
                case "ADMIN":
                    loadScene("admin-dashboard.fxml");
                    break;
                case "STAFF":
                    loadScene("staff-dashboard.fxml");
                    break;
                default:
                    // STUDENT -> chuyển qua student-dashboard.fxml
                    loadScene("student-dashboard.fxml");
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Lỗi hệ thống!");
        }
    }

    private void loadScene(String fxmlFile) {
        try {
            String path = "/fxml/" + fxmlFile;

            var url = getClass().getResource(path);
            if (url == null) {
                throw new RuntimeException("Không tìm thấy FXML: " + path);
            }

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("UCOP Education - " + CurrentUser.getCurrentAccount().getRole());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
