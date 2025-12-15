package com.ucop.edu.controller;

import com.ucop.edu.entity.Enrollment;
import com.ucop.edu.entity.Payment;
import com.ucop.edu.entity.Refund;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.entity.enums.PaymentStatus;
import com.ucop.edu.repository.EnrollmentRepository;
import com.ucop.edu.repository.PaymentRepository;
import com.ucop.edu.repository.RefundRepository;
import com.ucop.edu.service.impl.PaymentService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.List;

public class PaymentManagementController {

    // ===== Payments table
    @FXML private TableView<Payment> tblPayments;
    @FXML private TableColumn<Payment, Long> colPayId;
    @FXML private TableColumn<Payment, String> colEnroll;
    @FXML private TableColumn<Payment, String> colStudent;
    @FXML private TableColumn<Payment, String> colAmount;
    @FXML private TableColumn<Payment, String> colMethod;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, String> colPaidAt;

    // ===== Refunds table
    @FXML private TableView<Refund> tblRefunds;
    @FXML private TableColumn<Refund, Long> colRefundId;
    @FXML private TableColumn<Refund, String> colRefundAmount;
    @FXML private TableColumn<Refund, String> colRefundStatus;
    @FXML private TableColumn<Refund, String> colRefundAt;

    // ===== Filters
    @FXML private ComboBox<PaymentStatus> cbFilterStatus;
    @FXML private ComboBox<PaymentMethod> cbFilterMethod;
    @FXML private TextField txtSearch;

    // ===== Form: Payment
    @FXML private ComboBox<Enrollment> cbEnrollment;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<PaymentMethod> cbMethod;
    @FXML private ComboBox<PaymentStatus> cbPayStatus;
    @FXML private TextField txtTransactionId;

    // ===== Form: Refund
    @FXML private TextField txtRefundAmount;
    @FXML private TextArea txtRefundReason;

    @FXML private Label lblMsg;

    private final ObservableList<Payment> paymentData = FXCollections.observableArrayList();
    private final ObservableList<Refund> refundData = FXCollections.observableArrayList();

    private final EnrollmentRepository enrollmentRepo = new EnrollmentRepository();
    private final PaymentRepository paymentRepo = new PaymentRepository();
    private final RefundRepository refundRepo = new RefundRepository();
    private final PaymentService paymentService = new PaymentService();

    @FXML
    public void initialize() {
        cbMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cbPayStatus.setItems(FXCollections.observableArrayList(PaymentStatus.values()));
        cbFilterMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cbFilterStatus.setItems(FXCollections.observableArrayList(PaymentStatus.values()));

        cbEnrollment.setConverter(new StringConverter<>() {
            @Override public String toString(Enrollment e) {
                if (e == null) return "";
                String code = safe(e.getEnrollmentCode());
                String user = (e.getStudent() != null ? safe(e.getStudent().getUsername()) : "");
                return "#" + e.getId() + " - " + code + (user.isEmpty() ? "" : " - " + user);
            }
            @Override public Enrollment fromString(String s) { return null; }
        });

        colPayId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colEnroll.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEnrollment() == null ? "" : safe(c.getValue().getEnrollment().getEnrollmentCode())
        ));
        colStudent.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().getEnrollment() != null && c.getValue().getEnrollment().getStudent() != null)
                        ? safe(c.getValue().getEnrollment().getStudent().getUsername())
                        : ""
        ));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAmount() == null ? "0" : c.getValue().getAmount().toPlainString()
        ));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaymentMethod() == null ? "" : c.getValue().getPaymentMethod().name()
        ));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()
        ));
        colPaidAt.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaidAt() == null ? "" : c.getValue().getPaidAt().toString()
        ));

        colRefundId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colRefundAmount.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAmount() == null ? "0" : c.getValue().getAmount().toPlainString()
        ));
        colRefundStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()
        ));
        colRefundAt.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProcessedAt() == null ? "" : c.getValue().getProcessedAt().toString()
        ));

        tblPayments.setItems(paymentData);
        tblRefunds.setItems(refundData);

        tblPayments.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            fillPaymentForm(newV);
            loadRefunds(newV);
        });

        loadEnrollments();
        loadPayments();
        lblMsg.setText("");
    }

    // ===================== LOAD =====================
    private void loadEnrollments() {
        List<Enrollment> list = enrollmentRepo.findAllWithStudent();
        cbEnrollment.setItems(FXCollections.observableArrayList(list));
    }

    private void loadPayments() {
        paymentData.setAll(paymentRepo.findAllWithEnrollmentStudent());
    }

    private void loadRefunds(Payment p) {
        refundData.clear();
        if (p == null) return;
        refundData.setAll(refundRepo.findByPaymentId(p.getId()));
    }

    // ===================== FILTER =====================
    @FXML
    private void handleFilter() {
        // bạn muốn lọc nâng cao thì mình sẽ mở rộng query tiếp,
        // hiện tại: reset về load all để đảm bảo không lỗi.
        loadPayments();
        lblMsg.setText("✅ Đã tải lại danh sách (lọc nâng cao bạn bảo mình sẽ thêm tiếp)");
    }

    @FXML
    private void handleReset() {
        cbFilterStatus.setValue(null);
        cbFilterMethod.setValue(null);
        txtSearch.clear();
        loadPayments();
        lblMsg.setText("✅ Đã reset");
    }

    // ===================== PAYMENT =====================
    @FXML
    private void handleCreatePayment() {
        try {
            Enrollment e = cbEnrollment.getValue();
            if (e == null) { showError("Bạn chưa chọn Enrollment"); return; }

            BigDecimal amount = parseMoney(txtAmount.getText());
            if (amount == null || amount.signum() <= 0) { showError("Amount không hợp lệ"); return; }

            PaymentMethod method = cbMethod.getValue();
            if (method == null) { showError("Bạn chưa chọn Method"); return; }

            PaymentStatus status = cbPayStatus.getValue();
            String txId = txtTransactionId.getText();

            paymentService.createPayment(e.getId(), amount, method, status, txId);

            lblMsg.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            lblMsg.setText("✅ Tạo payment thành công");
            loadPayments();
            handleClearForm();
        } catch (Exception ex) {
            showError("Create payment lỗi: " + ex.getMessage());
        }
    }

    @FXML
    private void handleMarkPaid() {
        Payment selected = tblPayments.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Chọn 1 payment để mark PAID"); return; }

        try {
            paymentService.markPaid(selected.getId());
            lblMsg.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            lblMsg.setText("✅ Mark PAID thành công");
            loadPayments();
        } catch (Exception ex) {
            showError("Mark PAID lỗi: " + ex.getMessage());
        }
    }

    // ===================== REFUND =====================
    @FXML
    private void handleRequestRefund() {
        Payment pSel = tblPayments.getSelectionModel().getSelectedItem();
        if (pSel == null) { showError("Chọn 1 payment để refund"); return; }

        BigDecimal amount = parseMoney(txtRefundAmount.getText());
        if (amount == null || amount.signum() <= 0) { showError("Refund amount không hợp lệ"); return; }

        String reason = txtRefundReason.getText();

        try {
            paymentService.requestRefund(pSel.getId(), amount, reason);
            lblMsg.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            lblMsg.setText("✅ Đã tạo yêu cầu refund");
            loadRefunds(pSel);
            txtRefundAmount.clear();
            txtRefundReason.clear();
        } catch (Exception ex) {
            showError("Request refund lỗi: " + ex.getMessage());
        }
    }

    @FXML
    private void handleProcessRefund() {
        Refund rSel = tblRefunds.getSelectionModel().getSelectedItem();
        if (rSel == null) { showError("Chọn 1 refund để process"); return; }

        try {
            paymentService.processRefund(rSel.getId());
            lblMsg.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            lblMsg.setText("✅ Process refund OK");
            loadPayments();

            Payment pSel = tblPayments.getSelectionModel().getSelectedItem();
            if (pSel != null) loadRefunds(pSel);

        } catch (Exception ex) {
            showError("Process refund lỗi: " + ex.getMessage());
        }
    }

    // ===================== UI =====================
    private void fillPaymentForm(Payment p) {
        if (p == null) return;
        cbEnrollment.setValue(p.getEnrollment());
        cbMethod.setValue(p.getPaymentMethod());
        cbPayStatus.setValue(p.getStatus());
        txtAmount.setText(p.getAmount() == null ? "" : p.getAmount().toPlainString());
        txtTransactionId.setText(p.getTransactionId() == null ? "" : p.getTransactionId());
    }

    @FXML
    private void handleClearForm() {
        cbEnrollment.setValue(null);
        cbMethod.setValue(null);
        cbPayStatus.setValue(null);
        txtAmount.clear();
        txtTransactionId.clear();
        txtRefundAmount.clear();
        txtRefundReason.clear();
        lblMsg.setText("");
    }

    private void showError(String msg) {
        lblMsg.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        lblMsg.setText("❌ " + msg);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private BigDecimal parseMoney(String text) {
        try {
            if (text == null) return null;
            String t = text.trim().replace(",", "");
            if (t.isEmpty()) return null;
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void backToDashboard() {
        // Nếu bạn có sẵn hàm chuyển scene ở module khác -> copy y hệt qua đây.
        // Mình để an toàn không crash nếu bạn chưa nối scene.
        lblMsg.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
        lblMsg.setText("ℹ backToDashboard(): bạn nối scene giống module Course/Category là xong.");
    }
}
