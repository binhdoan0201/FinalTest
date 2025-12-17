package com.ucop.edu.controller;

import com.ucop.edu.entity.*;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.entity.enums.PromotionDiscountType;
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
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class StudentPaymentController {

    @FXML private ComboBox<Order> cbOrder;

    @FXML private Label lblPaid;
    @FXML private Label lblDue;

    @FXML private ComboBox<Promotion> cbPromo;
    @FXML private TextField txtPromoCode;
    @FXML private Label lblDiscount;

    @FXML private Label lblSubtotal;
    @FXML private Label lblFinalTotal;

    @FXML private ComboBox<PaymentMethod> cbMethod;
    @FXML private TextField txtAmount;
    @FXML private TextField txtTransactionId;
    @FXML private Label lblMsg;

    @FXML private Label lblWalletTop;

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

        cbPromo.setConverter(new StringConverter<>() {
            @Override public String toString(Promotion p) {
                if (p == null) return "";
                return safe(p.getCode()) + " - " + safe(p.getName());
            }
            @Override public Promotion fromString(String s) { return null; }
        });

        cbPromo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) txtPromoCode.setText(val.getCode());
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

        Long targetOrderId = StudentDashboardController.consumeTargetOrderId();
        if (targetOrderId != null) reSelectOrder(targetOrderId);
    }

    @FXML
    private void handlePay() {
        Long sid = currentStudentId();
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
                    sid, o.getId(), amount, method,
                    (txtTransactionId.getText() == null ? null : txtTransactionId.getText().trim())
            );

            setOk("✅ Thanh toán thành công: " + vnd.format(amount) + " VNĐ");
            txtAmount.clear();
            txtTransactionId.clear();

            handleRefresh();
            reSelectOrder(o.getId());
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        }
    }

    // =========================
    // APPLY / CLEAR PROMO (✅ KHÔNG trừ lượt ở đây)
    // =========================
    @FXML
    private void handleApplyPromo() {
        Long sid = currentStudentId();
        if (sid == null) { setErr("Bạn chưa đăng nhập."); return; }

        Order o = cbOrder.getValue();
        if (o == null) { setErr("Chưa chọn đơn."); return; }

        String code = (txtPromoCode.getText() == null) ? "" : txtPromoCode.getText().trim();
        if (code.isEmpty()) { setErr("Nhập mã giảm giá."); return; }

        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Order mo = fetchOrderForPayment(s, o.getId());
            if (mo == null) throw new IllegalStateException("Order không tồn tại.");

            Enrollment en = mo.getEnrollment();
            if (en == null) { tx.rollback(); setErr("Order chưa link Enrollment nên không áp mã được."); return; }

            BigDecimal paid = nvl(paymentRepo.sumPaidByOrder(s, mo.getId()));
            if (paid.signum() > 0) { tx.rollback(); setErr("Đơn đã có thanh toán, không thể áp mã."); return; }

            if (en.getPromotion() != null) { tx.rollback(); setErr("Đơn này đã áp mã rồi. Bấm 'Bỏ mã' nếu muốn đổi."); return; }

            Promotion promo = s.createQuery("select p from Promotion p where upper(p.code)=:c", Promotion.class)
                    .setParameter("c", code.toUpperCase())
                    .uniqueResult();
            if (promo == null) { tx.rollback(); setErr("Mã khuyến mãi không tồn tại."); return; }

            Boolean applyToAll = promo.isApplyToAll();
            if (applyToAll == null) applyToAll = true;
            if (!applyToAll) { tx.rollback(); setErr("Voucher này chỉ áp dụng cho 1 khóa học cụ thể (item-level)."); return; }

            LocalDate today = LocalDate.now();
            if (promo.getValidFrom() != null && today.isBefore(promo.getValidFrom())) { tx.rollback(); setErr("Mã chưa đến ngày áp dụng."); return; }
            if (promo.getValidTo() != null && today.isAfter(promo.getValidTo())) { tx.rollback(); setErr("Mã đã hết hạn."); return; }

            // ✅ chỉ CHECK còn lượt (KHÔNG tăng usedCount ở đây)
            int used = promo.getUsedCount() == null ? 0 : promo.getUsedCount();
            Integer max = promo.getMaxUsage();
            if (max != null && used >= max) { tx.rollback(); setErr("Mã đã hết lượt sử dụng."); return; }

            if (promo.getDiscountType() == null) { tx.rollback(); setErr("Voucher thiếu discount_type."); return; }
            if (promo.getDiscountValue() == null || promo.getDiscountValue().signum() <= 0) { tx.rollback(); setErr("Voucher thiếu/ sai discount_value."); return; }

            // baseSubtotal = tổng trước giảm (lấy từ Order hiện tại)
            BigDecimal baseSubtotal = nvl(mo.getTotalAmount());
            if (baseSubtotal.signum() <= 0) { tx.rollback(); setErr("Tổng tiền đơn không hợp lệ."); return; }

            BigDecimal discount = computeDiscount(baseSubtotal, promo.getDiscountType(), promo.getDiscountValue());
            if (discount.signum() <= 0) { tx.rollback(); setErr("Giảm giá không hợp lệ."); return; }

            // ✅ attach promo vào Enrollment để hiển thị, KHÔNG ghi PromotionUsage, KHÔNG tăng usedCount
            en.setSubtotal(baseSubtotal);
            en.setPromotion(promo);
            en.setDiscountAmount(discount);
            en.recalculateTotals();
            s.merge(en);

            mo.setTotalAmount(nvl(en.getTotalAmount()));
            s.merge(mo);

            tx.commit();

            setOk("✅ Đã áp mã, giảm " + vnd.format(discount) + " VNĐ");
            handleRefresh();
            reSelectOrder(o.getId());

        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
            setErr("Áp mã lỗi: " + ex.getMessage());
        }
    }

    @FXML
    private void handleClearPromo() {
        Order o = cbOrder.getValue();
        if (o == null) { setErr("Chưa chọn đơn."); return; }

        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Order mo = fetchOrderForPayment(s, o.getId());
            if (mo == null) throw new IllegalStateException("Order không tồn tại.");

            Enrollment en = mo.getEnrollment();
            if (en == null) { tx.rollback(); setErr("Order chưa link Enrollment nên không bỏ mã được."); return; }

            BigDecimal paid = nvl(paymentRepo.sumPaidByOrder(s, mo.getId()));
            if (paid.signum() > 0) { tx.rollback(); setErr("Đơn đã có thanh toán, không thể bỏ mã."); return; }

            // ✅ restore về baseSubtotal đã lưu trong Enrollment.subtotal
            BigDecimal baseSubtotal = nvl(en.getSubtotal());
            if (baseSubtotal.signum() <= 0) baseSubtotal = nvl(mo.getTotalAmount());

            en.setPromotion(null);
            en.setDiscountAmount(BigDecimal.ZERO);
            en.setSubtotal(baseSubtotal);
            en.recalculateTotals();
            s.merge(en);

            mo.setTotalAmount(nvl(en.getTotalAmount()));
            s.merge(mo);

            tx.commit();

            txtPromoCode.clear();
            cbPromo.setValue(null);

            setOk("✅ Đã bỏ mã giảm giá.");
            handleRefresh();
            reSelectOrder(o.getId());

        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            ex.printStackTrace();
            setErr("Bỏ mã lỗi: " + ex.getMessage());
        }
    }


    // =========================
    // REFRESH / LOAD
    // =========================
    @FXML
    private void handleRefresh() {
        Long sid = currentStudentId();
        if (sid == null) {
            cbOrder.setItems(FXCollections.observableArrayList());
            payData.clear();
            setMoney(lblPaid, BigDecimal.ZERO);
            setMoney(lblDue, BigDecimal.ZERO);
            setMoney(lblDiscount, BigDecimal.ZERO);
            setMoney(lblSubtotal, BigDecimal.ZERO);
            setMoney(lblFinalTotal, BigDecimal.ZERO);
            if (lblWalletTop != null) lblWalletTop.setText("0 VNĐ");
            return;
        }

        Long keepId = cbOrder.getValue() == null ? null : cbOrder.getValue().getId();

        loadOrdersNeedPay(sid);
        loadMyPayments(sid);
        loadWalletBalance(sid);
        loadAvailablePromos();

        if (keepId != null) reSelectOrder(keepId);
        updateSummary(cbOrder.getValue());
    }

    private void loadOrdersNeedPay(Long sid) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Order> list = s.createQuery(
                            "select o from Order o " +
                                    "join fetch o.student st " +
                                    "left join fetch o.enrollment e " +
                                    "left join fetch e.promotion " +
                                    "where st.id = :sid " +
                                    "and o.status in ('PENDING','PENDING_PAYMENT') " +
                                    "order by o.id desc",
                            Order.class
                    )
                    .setParameter("sid", sid)
                    .list();

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

    private void loadAvailablePromos() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate today = LocalDate.now();
            List<Promotion> list = s.createQuery(
                            "select p from Promotion p " +
                                    "where (p.applyToAll = true or p.applyToAll is null) " +
                                    "and (p.validFrom is null or p.validFrom <= :today) " +
                                    "and (p.validTo is null or p.validTo >= :today) " +
                                    // ✅ chỉ lấy promo còn lượt (hoặc unlimited)
                                    "and (p.maxUsage is null or coalesce(p.usedCount,0) < p.maxUsage) " +
                                    "order by p.id desc",
                            Promotion.class
                    )
                    .setParameter("today", today)
                    .list();
            cbPromo.setItems(FXCollections.observableArrayList(list));
        } catch (Exception ignored) {}
    }

    // =========================
    // SUMMARY
    // =========================
    private void updateSummary(Order selected) {
        if (selected == null || selected.getId() == null) {
            setMoney(lblPaid, BigDecimal.ZERO);
            setMoney(lblDue, BigDecimal.ZERO);
            setMoney(lblDiscount, BigDecimal.ZERO);
            setMoney(lblSubtotal, BigDecimal.ZERO);
            setMoney(lblFinalTotal, BigDecimal.ZERO);
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Order mo = fetchOrderForPayment(s, selected.getId());
            if (mo == null) return;

            BigDecimal finalTotal = nvl(mo.getTotalAmount());
            BigDecimal paid = nvl(paymentRepo.sumPaidByOrder(s, mo.getId()));
            BigDecimal due = finalTotal.subtract(paid);
            if (due.signum() < 0) due = BigDecimal.ZERO;

            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal subtotal = finalTotal;

            Enrollment en = mo.getEnrollment();
            if (en != null) {
                discount = nvl(en.getDiscountAmount());
                subtotal = nvl(en.getSubtotal());
                if (subtotal.signum() <= 0) subtotal = finalTotal.add(discount);

                if (en.getPromotion() != null && txtPromoCode != null) {
                    txtPromoCode.setText(en.getPromotion().getCode());
                }
            }

            setMoney(lblSubtotal, subtotal);
            setMoney(lblDiscount, discount);
            setMoney(lblFinalTotal, finalTotal);
            setMoney(lblPaid, paid);
            setMoney(lblDue, due);
        }
    }

    private Order fetchOrderForPayment(Session s, Long orderId) {
        return s.createQuery(
                        "select o from Order o " +
                                "left join fetch o.enrollment e " +
                                "left join fetch e.promotion " +
                                "where o.id = :oid", Order.class)
                .setParameter("oid", orderId)
                .uniqueResult();
    }

    private void reSelectOrder(Long orderId) {
        for (Order x : cbOrder.getItems()) {
            if (x != null && x.getId() != null && x.getId().equals(orderId)) {
                cbOrder.setValue(x);
                return;
            }
        }
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

    private BigDecimal computeDiscount(BigDecimal total, PromotionDiscountType type, BigDecimal value) {
        if (total == null || value == null || type == null) return BigDecimal.ZERO;
        if (total.signum() <= 0 || value.signum() <= 0) return BigDecimal.ZERO;

        String t = type.name().toUpperCase();
        if (t.contains("PERCENT")) {
            BigDecimal pct = value.divide(new BigDecimal("100"));
            BigDecimal d = total.multiply(pct);
            if (d.signum() < 0) d = BigDecimal.ZERO;
            return d.min(total);
        }
        return value.min(total);
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

    private Long currentStudentId() {
        return (CurrentUser.getCurrentAccount() == null) ? null : CurrentUser.getCurrentAccount().getId();
    }

    private void setMoney(Label lbl, BigDecimal amount) {
        if (lbl == null) return;
        lbl.setText(vnd.format(nvl(amount)) + " VNĐ");
    }

    private BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private String safe(String s) { return s == null ? "" : s; }

    private void setOk(String msg) {
        lblMsg.setStyle("-fx-text-fill:#22c55e; -fx-font-weight:900;");
        lblMsg.setText("✓ " + msg);
    }
    private void setErr(String msg) {
        lblMsg.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:900;");
        lblMsg.setText("✗ " + msg);
    }
}
