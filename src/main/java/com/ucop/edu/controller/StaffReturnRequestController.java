package com.ucop.edu.controller;

import com.ucop.edu.entity.ReturnRequest;
import com.ucop.edu.service.impl.ReturnRequestService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

public class StaffReturnRequestController {

    @FXML private TableView<ReturnRequest> tbl;
    @FXML private TableColumn<ReturnRequest, Long> colId;
    @FXML private TableColumn<ReturnRequest, String> colOrder;
    @FXML private TableColumn<ReturnRequest, String> colStudent;
    @FXML private TableColumn<ReturnRequest, String> colType;
    @FXML private TableColumn<ReturnRequest, String> colAmount;
    @FXML private TextField txtApproveAmount;
    @FXML private TextArea txtNote;
    @FXML private Label lblMsg;

    private final ReturnRequestService service = new ReturnRequestService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colOrder.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getOrder() == null ? "" : ("#" + c.getValue().getOrder().getId())
        ));
        colStudent.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getStudent() == null ? "" : c.getValue().getStudent().getUsername()
        ));
        colType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getType() == null ? "" : c.getValue().getType().name()
        ));
        colAmount.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getRefundAmount() == null ? "FULL/AUTO" : c.getValue().getRefundAmount().toPlainString()
        ));

        reload();
    }

    @FXML
    public void reload() {
        tbl.setItems(FXCollections.observableArrayList(service.listRequested()));
        lblMsg.setText("");
        txtApproveAmount.clear();
        txtNote.clear();
    }

    @FXML
    public void approve() {
        ReturnRequest rr = tbl.getSelectionModel().getSelectedItem();
        if (rr == null) { lblMsg.setText("Chọn 1 ticket"); return; }

        BigDecimal amount = null;
        try {
            String t = txtApproveAmount.getText();
            if (t != null && !t.trim().isEmpty()) amount = new BigDecimal(t.trim().replace(",", "").replace(".", ""));
        } catch (Exception ignore) {}

        service.approve(rr.getId(), amount, txtNote.getText());
        lblMsg.setText("✅ Approved");
        reload();
    }

    @FXML
    public void reject() {
        ReturnRequest rr = tbl.getSelectionModel().getSelectedItem();
        if (rr == null) { lblMsg.setText("Chọn 1 ticket"); return; }

        service.reject(rr.getId(), txtNote.getText());
        lblMsg.setText("✅ Rejected");
        reload();
    }

    @FXML
    public void process() {
        ReturnRequest rr = tbl.getSelectionModel().getSelectedItem();
        if (rr == null) { lblMsg.setText("Chọn 1 ticket"); return; }

        service.processToWallet(rr.getId());
        lblMsg.setText("✅ Processed to wallet");
        reload();
    }
}
