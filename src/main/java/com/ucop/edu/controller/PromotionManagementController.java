package com.ucop.edu.controller;

import com.ucop.edu.entity.Promotion;
import com.ucop.edu.service.impl.PromotionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PromotionManagementController {

    @FXML private TableView<Promotion> table;
    @FXML private TableColumn<Promotion, Long> colId;
    @FXML private TableColumn<Promotion, String> colCode;
    @FXML private TableColumn<Promotion, String> colName;
    @FXML private TableColumn<Promotion, String> colType;
    @FXML private TableColumn<Promotion, String> colValue;
    @FXML private TableColumn<Promotion, String> colFrom;
    @FXML private TableColumn<Promotion, String> colTo;
    @FXML private TableColumn<Promotion, String> colScope;

    // ✅ Cột còn lượt (maxUsage - usedCount), chỉ trừ khi PAY xong
    @FXML private TableColumn<Promotion, String> colRemaining;

    @FXML private TextField keywordField;

    private final PromotionService promotionService = new PromotionService();
    private final ObservableList<Promotion> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colCode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nvl(c.getValue().getCode())));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nvl(c.getValue().getName())));

        colType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDiscountType() == null ? "" : c.getValue().getDiscountType().name()
        ));

        colValue.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDiscountValue() == null ? "" : c.getValue().getDiscountValue().toPlainString()
        ));

        colFrom.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getValidFrom() == null ? "" : c.getValue().getValidFrom().toString()
        ));

        colTo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getValidTo() == null ? "" : c.getValue().getValidTo().toString()
        ));

        colScope.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().isApplyToAll())
                        ? "CART"
                        : ("ITEM(courseId=" + (c.getValue().getCourse() == null ? "?" : c.getValue().getCourse().getId()) + ")")
        ));

        if (colRemaining != null) {
            colRemaining.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                    c.getValue() == null ? "" : c.getValue().getRemainingUsageText()
            ));
        }

        table.setItems(data);
        reload();
    }

    @FXML
    public void reload() {
        data.setAll(promotionService.findAll());
    }

    @FXML
    public void onSearch() {
        String k = keywordField == null ? null : keywordField.getText();
        if (k == null || k.trim().isEmpty()) {
            reload();
            return;
        }
        String kw = k.trim().toLowerCase();
        data.setAll(promotionService.findAll().stream()
                .filter(p -> (p.getCode() != null && p.getCode().toLowerCase().contains(kw))
                        || (p.getName() != null && p.getName().toLowerCase().contains(kw)))
                .toList());
    }

    @FXML
    public void onNew() {
        PromotionFormController.open(null);
        reload();
    }

    @FXML
    public void onEdit() {
        Promotion selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Chọn 1 promotion để sửa.");
            return;
        }
        PromotionFormController.open(selected.getId());
        reload();
    }

    @FXML
    public void onExpireNow() {
        Promotion selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Chọn 1 promotion để expire.");
            return;
        }
        promotionService.expireNow(selected.getId());
        reload();
    }

    @FXML
    public void onDelete() {
        Promotion selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Chọn 1 promotion để xóa.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa promotion này?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                promotionService.delete(selected.getId());
                reload();
            }
        });
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    @FXML
    private void backToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UCOP - Admin Dashboard");
            stage.centerOnScreen();
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
