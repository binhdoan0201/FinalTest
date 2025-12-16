package com.ucop.edu.controller;

import com.ucop.edu.entity.Order;
import com.ucop.edu.entity.Payment;
import com.ucop.edu.entity.Wallets;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.repository.PaymentRepository;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.service.impl.PaymentService;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class StudentPaymentController {

    @FXML private ComboBox<Order> cbOrder;

    @FXML private Label lblTotal;
    @FXML private Label lblPaid;
    @FXML private Label lblDue;

    @FXML private ComboBox<PaymentMethod> cbMethod;
    @FXML private TextField txtAmount;
    @FXML private TextField txtTransactionId;
    @FXML private Label lblMsg;

    @FXML private Label lblWalletTop; // góc phải "Ví: ..."

    @FXML private TableView<Payment> tblPayments;
    @FXML private TableColumn<Payment, Long> colPayId;
    @FXML private TableColumn<Payment, String> colOrderId;
    @FXML private TableColumn<Payment, String> colPayAmount;
    @FXML private TableColumn<Payment, String> colPayMethod;
    @FXML private TableColumn<Payment, String> colPayStatus;
    @FXML private TableColumn<Payment, String> colPayAt;

    private final PaymentService paymentService = new PaymentService();
    private final PaymentRepository paymentRepo = new PaymentRepository();
    private final WalletRepository walletRepo = new WalletRepository();

    private final ObservableList<Payment> payData = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        cbMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cbMethod.setValue(PaymentMethod.WALLET);

        cbOrder.setConverter(new StringConverter<>() {
            @Override public String toString(Order o) {
                if (o == null) return "";
                BigDecimal total = nvl(o.getTotalAmount());
                BigDecimal paid = calcPaid(o.getId());
                BigDecimal due = total.subtract(paid);
                if (due.signum() < 0) due = BigDecimal.ZERO;
                return "Order #" + o.getId() + " | Còn nợ: " + vnd.format(due) + " VNĐ | " + safe(o.getStatus());
            }
            @Override public Order fromString(String s) { return null; }
        });

        cbOrder.valueProperty().addListener((obs, old, val) -> updateSummary(val));

        colPayId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colOrderId.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrder() == null ? "" : ("#" + c.getValue().getOrder().getId())
        ));
        colPayAmount.setCellValueFactory(c -> new SimpleStringProperty(vnd.format(nvl(c.getValue().getAmount())) + " VNĐ"));
        colPayMethod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaymentMethod() == null ? "" : c.getValue().getPaymentMethod().name()
        ));
        colPayStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()
        ));
        colPayAt.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaidAt() == null ? "" : dtf.format(c.getValue().getPaidAt())
        ));

        tblPayments.setItems(payData);

        handleRefresh();

        // ✅ auto chọn order vừa checkout
        Long targetOrderId = StudentDashboardController.consumeTargetOrderId();
        if (targetOrderId != null) {
            for (Order o : cbOrder.getItems()) {
                if (o != null && o.getId() != null && o.getId().equals(targetOrderId)) {
                    cbOrder.setValue(o);
                    break;
                }
            }
        }
    }

    @FXML
    private void handlePay() {
        Long sid = (CurrentUser.getCurrentAccount() == null) ? null : CurrentUser.getCurrentAccount().getId();
        if (sid == null) { setErr("Bạn chưa đăng nhập."); return; }

        Order o = cbOrder.getValue();
        if (o == null) { setErr("Bạn chưa chọn đơn hàng."); return; }

        PaymentMethod method = cbMethod.getValue();
        if (method == null) { setErr("Bạn chưa chọn phương thức."); return; }

        BigDecimal due = calcDue(o.getId());
        if (due.signum() <= 0) { setErr("Đơn này đã thanh toán đủ."); return; }

        BigDecimal amount = parseMoney(txtAmount.getText());
        if (txtAmount.getText() == null || txtAmount.getText().trim().isEmpty()) amount = due;
        if (amount == null || amount.signum() <= 0) { setErr("Số tiền không hợp lệ."); return; }
        if (amount.compareTo(due) > 0) { setErr("Số tiền vượt quá phần còn lại cần trả."); return; }

        try {
            paymentService.payOrder(
                    sid,
                    o.getId(),
                    amount,
                    method,
                    (txtTransactionId.getText() == null ? null : txtTransactionId.getText().trim())
            );

            setOk("✅ Thanh toán thành công: " + vnd.format(amount) + " VNĐ");
            txtAmount.clear();
            txtTransactionId.clear();

            handleRefresh(); // reload orders + history + wallet
            // giữ selection order
            reSelectOrder(o.getId());

        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        Long sid = (CurrentUser.getCurrentAccount() == null) ? null : CurrentUser.getCurrentAccount().getId();
        if (sid == null) {
            cbOrder.setItems(FXCollections.observableArrayList());
            payData.clear();
            lblTotal.setText("0 VNĐ");
            lblPaid.setText("0 VNĐ");
            lblDue.setText("0 VNĐ");
            if (lblWalletTop != null) lblWalletTop.setText("0 VNĐ");
            return;
        }

        loadOrdersNeedPay(sid);
        loadMyPayments(sid);
        loadWalletBalance(sid);

        updateSummary(cbOrder.getValue());
    }

    private void loadOrdersNeedPay(Long sid) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Order> list = s.createQuery(
                    "select o from Order o " +
                    "join fetch o.student st " +
                    "where st.id = :sid " +
                    "and o.status in ('PENDING','PENDING_PAYMENT') " +
                    "order by o.id desc",
                    Order.class
            ).setParameter("sid", sid).list();

            cbOrder.setItems(FXCollections.observableArrayList(list));
            if (!list.isEmpty() && cbOrder.getValue() == null) cbOrder.setValue(list.get(0));
        }
    }

    private void loadMyPayments(Long sid) {
        payData.setAll(paymentRepo.findByStudentFetchUI(sid));
    }

    private void loadWalletBalance(Long sid) {
        if (lblWalletTop == null) return;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Wallets w = walletRepo.getOrCreate(sid, s);
            lblWalletTop.setText(vnd.format(nvl(w.getBalance())) + " VNĐ");
        } catch (Exception e) {
            lblWalletTop.setText("0 VNĐ");
        }
    }

    private void updateSummary(Order o) {
        if (o == null) {
            lblTotal.setText("0 VNĐ");
            lblPaid.setText("0 VNĐ");
            lblDue.setText("0 VNĐ");
            return;
        }
        BigDecimal total = nvl(o.getTotalAmount());
        BigDecimal paid = calcPaid(o.getId());
        BigDecimal due  = total.subtract(paid);
        if (due.signum() < 0) due = BigDecimal.ZERO;

        lblTotal.setText(vnd.format(total) + " VNĐ");
        lblPaid.setText(vnd.format(paid) + " VNĐ");
        lblDue.setText(vnd.format(due) + " VNĐ");
    }

    private void reSelectOrder(Long orderId) {
        for (Order x : cbOrder.getItems()) {
            if (x != null && x.getId() != null && x.getId().equals(orderId)) {
                cbOrder.setValue(x);
                return;
            }
        }
        cbOrder.setValue(null);
    }

    private BigDecimal calcPaid(Long orderId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return nvl(paymentRepo.sumPaidByOrder(s, orderId));
        }
    }

    private BigDecimal calcDue(Long orderId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Order o = s.get(Order.class, orderId);
            if (o == null) return BigDecimal.ZERO;
            BigDecimal total = nvl(o.getTotalAmount());
            BigDecimal paid = nvl(paymentRepo.sumPaidByOrder(s, orderId));
            BigDecimal due = total.subtract(paid);
            return due.signum() < 0 ? BigDecimal.ZERO : due;
        }
    }

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

    private BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private String safe(String s) { return s == null ? "" : s; }

    private void setOk(String msg) {
        lblMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
        lblMsg.setText(msg);
    }
    private void setErr(String msg) {
        lblMsg.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:bold;");
        lblMsg.setText("❌ " + msg);
    }
}
