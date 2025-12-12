package com.ucop.edu.controller;

import com.ucop.edu.entity.*;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hibernate.Session;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class AdminUserController {

    @FXML private TableView<Account> userTable;
    @FXML private TableColumn<Account, Long> colId;
    @FXML private TableColumn<Account, String> colUsername;
    @FXML private TableColumn<Account, String> colFullName;
    @FXML private TableColumn<Account, String> colRole;
    @FXML private TableColumn<Account, String> colStatus;
    @FXML private TableColumn<Account, Void> colAction;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colFullName.setCellValueFactory(cellData -> {
            AccountProfile p = cellData.getValue().getProfile();
            return new javafx.beans.property.SimpleStringProperty(p != null ? p.getFullName() : "");
        });
        colStatus.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().isEnabled() ? "Hoạt động" : "Bị khóa"));

        colAction.setCellFactory(tc -> new ActionCell());
        refreshTable();
    }

    @FXML private void openAddUser() throws IOException { openUserForm(null); }
    @FXML private void refreshTable() { loadUsers(); }

    private void openUserForm(Account account) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin-add-user.fxml"));
        Parent root = loader.load();
        AdminUserFormController ctrl = loader.getController();
        if (account != null) {
            ctrl.setAccount(account);
            ctrl.setTitle("SỬA NGƯỜI DÙNG");
        } else {
            ctrl.setTitle("THÊM NGƯỜI DÙNG MỚI");
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root));
        stage.showAndWait();
        refreshTable();
    }

    private void loadUsers() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Account> list = s.createQuery("FROM Account", Account.class).list();
            userTable.setItems(FXCollections.observableArrayList(list));
        }
    }

    private void logAction(String action, String desc) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            var tx = s.beginTransaction();
            AuditLog log = new AuditLog();
            log.setUsername(CurrentUser.getUsername());
            log.setAction(action);
            log.setDescription(desc);
            log.setCreatedAt(LocalDateTime.now());
            s.save(log);
            tx.commit();
        } catch (Exception ignored) {}
    }

    private class ActionCell extends TableCell<Account, Void> {
        private final Button btnToggle = new Button();
        private final Button btnPass = new Button("Đổi MK");
        private final Button btnEdit = new Button("Sửa");
        private final Button btnDel = new Button("Xóa");

        {
            btnPass.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-size: 11px;");
            btnEdit.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-size: 11px;");
            btnDel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px;");

            btnPass.setOnAction(e -> changePassword(getTableRow().getItem()));
            btnEdit.setOnAction(e -> { try { openUserForm(getTableRow().getItem()); } catch (Exception ex) { ex.printStackTrace(); } });
            btnDel.setOnAction(e -> deleteUser(getTableRow().getItem()));
        }

        @Override protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            Account a = getTableRow().getItem();
            btnToggle.setText(a.isEnabled() ? "Khóa" : "Mở khóa");
            btnToggle.setStyle(a.isEnabled()
                    ? "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px;"
                    : "-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 11px;");
            btnToggle.setOnAction(e -> toggleLock(a));

            setGraphic(new HBox(6, btnToggle, btnPass, btnEdit, btnDel));
        }
    }

    private void toggleLock(Account a) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            var t = s.beginTransaction();
            Account acc = s.get(Account.class, a.getId());
            acc.setEnabled(!acc.isEnabled());
            s.update(acc);
            t.commit();
            logAction("TOGGLE_USER", (acc.isEnabled() ? "Mở khóa" : "Khóa") + " user: " + acc.getUsername());
            refreshTable();
        }
    }

    private void changePassword(Account a) {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("Đổi mật khẩu cho " + a.getUsername());
        d.showAndWait().ifPresent(p -> {
            if (!p.isEmpty()) {
                try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                    var t = s.beginTransaction();
                    Account acc = s.get(Account.class, a.getId());
                    acc.setPassword(p);
                    s.update(acc);
                    t.commit();
                    logAction("CHANGE_PASS", "Đổi mật khẩu user: " + a.getUsername());
                    new Alert(Alert.AlertType.INFORMATION, "Thành công!").show();
                }
            }
        });
    }
    @FXML
    private void backToDashboard() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
        Stage stage = (Stage) userTable.getScene().getWindow();
        
        // Set lại kích thước chuẩn như lần đầu mở Dashboard
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.centerOnScreen();
        
        stage.setScene(new Scene(root));
        stage.setTitle("UCOP - Universal Commerce & Operations Platform");
    }

    private void deleteUser(Account a) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Xóa " + a.getUsername() + "?");
        c.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                    var t = s.beginTransaction();
                    s.delete(a);
                    t.commit();
                    logAction("DELETE_USER", "Xóa user: " + a.getUsername());
                    refreshTable();
                }
            }
        });
    }
}