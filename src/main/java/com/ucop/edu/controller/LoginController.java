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
            Account account = session.createQuery("FROM Account WHERE username = :u", Account.class)
                    .setParameter("u", username)
                    .uniqueResult();

            if (account == null || !account.isEnabled()) {
                errorLabel.setText("Tài khoản không tồn tại hoặc bị khóa!");
                return;
            }

            // Tạm thời cho phép đăng nhập bằng mật khẩu gốc (chưa hash) để test
            // Sau này bạn thay bằng BCrypt.checkpw(password, account.getPassword())
            if (password.equals(account.getPassword())) {
                errorLabel.setText("");

                // Lưu user hiện tại (có thể dùng static hoặc Preference)
                CurrentUser.setCurrentAccount(account);

                if ("ADMIN".equalsIgnoreCase(account.getRole())) {
                    loadScene("/fxml/admin-dashboard.fxml");
                } else if ("STAFF".equalsIgnoreCase(account.getRole())) {
                	Parent root = FXMLLoader.load(getClass().getResource("/fxml/staff-dashboard.fxml"));
                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("UCOP Education - " + CurrentUser.getCurrentAccount().getRole());
                } else {
                    loadScene("/fxml/student-dashboard.fxml");
                }
            } else {
                errorLabel.setText("Sai mật khẩu!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Lỗi kết nối CSDL!");
        }
    }

    private void loadScene(String fxml) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root, 1200, 800));
        stage.setTitle("UCOP Education - " + CurrentUser.getCurrentAccount().getRole());
    }
}