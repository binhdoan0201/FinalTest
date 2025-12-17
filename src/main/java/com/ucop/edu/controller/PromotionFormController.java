package com.ucop.edu.controller;

import com.ucop.edu.entity.Promotion;
import com.ucop.edu.entity.enums.PromotionDiscountType;
import com.ucop.edu.service.impl.PromotionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class PromotionFormController {

    @FXML private Label titleLabel;

    @FXML private TextField codeField;
    @FXML private TextField nameField;

    @FXML private ComboBox<PromotionDiscountType> discountTypeBox;
    @FXML private TextField discountValueField;

    @FXML private TextField maxUsageField;
    @FXML private DatePicker validFromPicker;
    @FXML private DatePicker validToPicker;

    @FXML private CheckBox applyToAllCheck;
    @FXML private TextField courseIdField;

    private final PromotionService promotionService = new PromotionService();

    private Long editingId = null;

    public static void open(Long promotionIdOrNull) {
        try {
            FXMLLoader loader = new FXMLLoader(PromotionFormController.class.getResource("/fxml/promotion-form.fxml"));
            Scene scene = new Scene(loader.load());

            PromotionFormController c = loader.getController();
            c.load(promotionIdOrNull);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Promotion Form");
            st.setScene(scene);
            st.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Không mở được form: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void initialize() {
        discountTypeBox.setItems(FXCollections.observableArrayList(PromotionDiscountType.values()));

        applyToAllCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            courseIdField.setDisable(Boolean.TRUE.equals(newV));
            if (Boolean.TRUE.equals(newV)) courseIdField.clear();
        });

        // default
        applyToAllCheck.setSelected(true);
        courseIdField.setDisable(true);
    }

    public void load(Long promotionIdOrNull) {
        this.editingId = promotionIdOrNull;

        if (promotionIdOrNull == null) {
            titleLabel.setText("Create Promotion");
            return;
        }

        Promotion p = promotionService.findById(promotionIdOrNull);
        if (p == null) {
            alert("Promotion không tồn tại.");
            return;
        }

        titleLabel.setText("Edit Promotion #" + p.getId());
        codeField.setText(p.getCode());
        nameField.setText(p.getName());
        discountTypeBox.setValue(p.getDiscountType());
        discountValueField.setText(p.getDiscountValue() == null ? "" : p.getDiscountValue().toPlainString());
        maxUsageField.setText(p.getMaxUsage() == null ? "" : String.valueOf(p.getMaxUsage()));
        validFromPicker.setValue(p.getValidFrom());
        validToPicker.setValue(p.getValidTo());

        boolean applyToAll = Boolean.TRUE.equals(p.isApplyToAll());
        applyToAllCheck.setSelected(applyToAll);
        courseIdField.setDisable(applyToAll);
        if (!applyToAll && p.getCourse() != null) {
            courseIdField.setText(String.valueOf(p.getCourse().getId()));
        }
    }

    @FXML
    public void onSave() {
        try {
            Promotion p = (editingId == null) ? new Promotion() : promotionService.findById(editingId);
            if (p == null) p = new Promotion();

            p.setCode(codeField.getText());
            p.setName(nameField.getText());
            p.setDiscountType(discountTypeBox.getValue());

            BigDecimal discountValue = new BigDecimal(discountValueField.getText().trim());
            p.setDiscountValue(discountValue);

            String maxText = maxUsageField.getText();
            if (maxText == null || maxText.trim().isEmpty()) p.setMaxUsage(null);
            else p.setMaxUsage(Integer.parseInt(maxText.trim()));

            LocalDate vf = validFromPicker.getValue();
            LocalDate vt = validToPicker.getValue();
            p.setValidFrom(vf);
            p.setValidTo(vt);

            p.setApplyToAll(applyToAllCheck.isSelected());

            Long courseId = null;
            if (!applyToAllCheck.isSelected()) {
                if (courseIdField.getText() == null || courseIdField.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Item-level phải nhập courseId.");
                }
                courseId = Long.parseLong(courseIdField.getText().trim());
            }

            promotionService.saveOrUpdate(p, courseId);
            close();
        } catch (Exception e) {
            alert("Lỗi lưu: " + e.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        close();
    }

    private void close() {
        Stage st = (Stage) codeField.getScene().getWindow();
        st.close();
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

}
