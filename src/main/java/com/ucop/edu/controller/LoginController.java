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
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

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

            // Demo: so sánh plain text
            if (!password.equals(account.getPassword())) {
                errorLabel.setText("Sai mật khẩu!");
                return;
            }

            // Login OK
            CurrentUser.setCurrentAccount(account);
            errorLabel.setText("");

            String role = account.getRole() == null ? "" : account.getRole().toUpperCase();

            switch (role) {
                case "ADMIN" -> loadScene("admin-dashboard.fxml", "UCOP Education - ADMIN");
                case "STAFF" -> loadScene("staff-dashboard.fxml", "UCOP Education - STAFF");
                default -> loadScene("student-dashboard.fxml", "UCOP Education - STUDENT");
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Lỗi hệ thống!");
        }
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            String fxmlPath = "/fxml/" + fxmlFile;

            var url = getClass().getResource(fxmlPath);
            if (url == null) throw new RuntimeException("Không tìm thấy FXML: " + fxmlPath);

            Parent root = FXMLLoader.load(url);

            Scene scene = new Scene(root, 1200, 800);

            // ✅ GẮN CSS Ở ĐÂY
            addStylesheet(scene, "/css/student.css");   // UI xịn cho student
            addStylesheet(scene, "/css/app.css");       // (tuỳ chọn) nếu bạn có css chung

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Không thể load màn hình: " + fxmlFile);
        }
    }

    private void addStylesheet(Scene scene, String cssPath) {
        try {
            var cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
            }
        } catch (Exception ignore) {
            // không crash nếu thiếu css
        }
    }
}
