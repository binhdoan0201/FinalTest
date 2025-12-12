package com.ucop.edu.controller;

import com.ucop.edu.entity.*;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;

public class AdminUserFormController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtFullName;
    @FXML private ComboBox<String> cbRole;
    @FXML private Button btnSave;
    @FXML private Label lblTitle;

    private Account accountToEdit = null;

    @FXML
    private void initialize() {
        cbRole.getItems().addAll("ADMIN", "STAFF", "CUSTOMER");
        cbRole.setValue("CUSTOMER");
    }

    public void setAccount(Account acc) {
        this.accountToEdit = acc;
        txtUsername.setText(acc.getUsername());
        txtFullName.setText(acc.getProfile() != null ? acc.getProfile().getFullName() : "");
        cbRole.setValue(acc.getRole());
        txtPassword.setPromptText("Để trống nếu không đổi");
    }

    public void setTitle(String title) {
        lblTitle.setText(title);
    }

    @FXML
    private void saveUser() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText();
        String name = txtFullName.getText().trim();
        String role = cbRole.getValue();

        if (user.isEmpty() || role == null) {
            new Alert(Alert.AlertType.ERROR, "Vui lòng nhập đầy đủ!").show();
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction t = s.beginTransaction();
            if (accountToEdit == null) {
                Account a = new Account();
                a.setUsername(user);
                a.setPassword(pass.isEmpty() ? "123" : pass);
                a.setRole(role);
                a.setEnabled(true);
                AccountProfile p = new AccountProfile();
                p.setFullName(name);
                a.setProfile(p);
                s.save(a);
                log("ADD_USER", "Thêm: " + user);
            } else {
                Account a = s.get(Account.class, accountToEdit.getId());
                a.setUsername(user);
                a.setRole(role);
                if (!pass.isEmpty()) a.setPassword(pass);
                if (a.getProfile() != null) a.getProfile().setFullName(name);
                else {
                    AccountProfile p = new AccountProfile();
                    p.setFullName(name);
                    a.setProfile(p);
                }
                s.update(a);
                log("EDIT_USER", "Sửa: " + user);
            }
            t.commit();
            close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi: " + e.getMessage()).show();
        }
    }

    @FXML private void cancel() { close(); }

    private void close() {
        ((Stage) txtUsername.getScene().getWindow()).close();
    }

    private void log(String action, String desc) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            var t = s.beginTransaction();
            AuditLog l = new AuditLog();
            l.setUsername(CurrentUser.getUsername());
            l.setAction(action);
            l.setDescription(desc);
            l.setCreatedAt(LocalDateTime.now());
            s.save(l);
            t.commit();
        } catch (Exception ignored) {}
    }
}