package com.ucop.edu.controller;

import com.ucop.edu.entity.Account;
import com.ucop.edu.entity.AccountProfile;
import com.ucop.edu.entity.AuditLog;
import com.ucop.edu.util.HibernateUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;

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
            String name = p != null ? p.getFullName() : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        colStatus.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().isEnabled() ? "Hoạt động" : "Bị khóa"
            )
        );

        colAction.setCellFactory(tc -> new ActionCell());
        loadUsers();
    }

    private void loadUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var list = session.createQuery("FROM Account", Account.class).list();
            userTable.setItems(FXCollections.observableArrayList(list));
        }
    }

    private void logAction(String action, String desc) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            AuditLog log = new AuditLog();
            log.setUsername("admin");
            log.setAction(action);
            log.setDescription(desc);
            log.setCreatedAt(LocalDateTime.now());
            session.save(log);
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ActionCell extends TableCell<Account, Void> {
        private final Button btnToggle = new Button();
        private final Button btnChangePass = new Button("Đổi mật khẩu");

        public ActionCell() {
            btnChangePass.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black;");
            btnChangePass.setOnAction(e -> changePassword(getTableRow().getItem()));
            updateButton();
        }

        private void updateButton() {
            Account acc = getTableRow().getItem();
            if (acc == null) return;

            if (acc.isEnabled()) {
                btnToggle.setText("Khóa");
                btnToggle.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            } else {
                btnToggle.setText("Mở khóa");
                btnToggle.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
            }
            btnToggle.setOnAction(e -> toggleLock(acc));
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow().getItem() == null) {
                setGraphic(null);
            } else {
                updateButton();
                HBox box = new HBox(10, btnToggle, btnChangePass);
                setGraphic(box);
            }
        }
    }

    private void toggleLock(Account acc) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Account a = session.get(Account.class, acc.getId());
            a.setEnabled(!a.isEnabled());
            session.update(a);
            tx.commit();
            logAction("TOGGLE_USER", "Thay đổi trạng thái: " + a.getUsername());
            loadUsers();
        }
    }

    private void changePassword(Account acc) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText("Nhập mật khẩu mới cho: " + acc.getUsername());
        dialog.showAndWait().ifPresent(pass -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();
                Account a = session.get(Account.class, acc.getId());
                a.setPassword(pass);
                session.update(a);
                tx.commit();
                logAction("CHANGE_PASS", "Đổi mật khẩu cho: " + a.getUsername());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đổi mật khẩu thành công!");
                alert.show();
            }
        });
    }
}