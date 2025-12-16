package com.ucop.edu.controller;

import com.ucop.edu.entity.Enrollment;
import com.ucop.edu.entity.WalletTransaction;
import com.ucop.edu.entity.Wallets;
import com.ucop.edu.entity.enums.EnrollmentStatus;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.service.impl.WalletService;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StudentWalletController {

    @FXML private Label lblBalance;
    @FXML private TextField txtTopupAmount;
    @FXML private Label lblMsg;

    @FXML private TableView<TxRow> tblTx;
    @FXML private TableColumn<TxRow, String> colTime;
    @FXML private TableColumn<TxRow, String> colType;
    @FXML private TableColumn<TxRow, String> colAmount;
    @FXML private TableColumn<TxRow, String> colNote;

    private final WalletRepository walletRepo = new WalletRepository();
    private final WalletService walletService = new WalletService();

    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObservableList<TxRow> data = FXCollections.observableArrayList();

    public static class TxRow {
        private final LocalDateTime time;
        private final String type;
        private final BigDecimal amount;
        private final String note;

        public TxRow(LocalDateTime time, String type, BigDecimal amount, String note) {
            this.time = time;
            this.type = type;
            this.amount = amount;
            this.note = note;
        }
        public LocalDateTime getTime() { return time; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getNote() { return note; }
    }

    @FXML
    public void initialize() {
        colTime.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTime() == null ? "" : dtf.format(c.getValue().getTime()))
        );
        colType.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getType())));
        colAmount.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAmount() == null ? "" : (vnd.format(c.getValue().getAmount()) + " VNĐ"))
        );
        colNote.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNote())));

        tblTx.setItems(data);

        handleRefresh();
    }

    @FXML
    private void handleTopup() {
        try {
            Long aid = CurrentUser.getCurrentAccount().getId();
            BigDecimal amount = parseMoney(txtTopupAmount.getText());
            if (amount == null || amount.signum() <= 0) {
                setErr("Số tiền nạp không hợp lệ");
                return;
            }

            walletService.topup(aid, amount);

            setOk("✅ Nạp ví thành công");
            txtTopupAmount.clear();
            handleRefresh();

        } catch (Exception e) {
            e.printStackTrace();
            setErr("Nạp ví lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        Long studentId = (CurrentUser.getCurrentAccount() == null) ? null : CurrentUser.getCurrentAccount().getId();
        if (studentId == null) {
            lblBalance.setText("0 VNĐ");
            lblMsg.setText("");
            data.clear();
            return;
        }
        loadWalletAndHistory(studentId);
    }

    private void loadWalletAndHistory(Long studentId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            Wallets w = walletRepo.getOrCreate(studentId, s);
            lblBalance.setText(vnd.format(w.getBalance()) + " VNĐ");

            // ✅ SỬA TRIỆT ĐỂ: KHÔNG dùng t.accountId nữa
            List<WalletTransaction> txs = s.createQuery(
            	    "select t from WalletTransaction t " +
            	    "where t.account.id = :sid " +
            	    "order by t.createdAt desc",
            	    WalletTransaction.class
            	).setParameter("sid", studentId).list();

            List<TxRow> rows = new ArrayList<>();
            for (WalletTransaction t : txs) {
                String note = nvl(t.getNote());
                if (note.isBlank()) note = nvl(t.getMessage());

                rows.add(new TxRow(
                        t.getCreatedAt(),
                        t.getType() == null ? "" : t.getType().name(),
                        t.getAmount(),
                        note
                ));
            }

            // thông báo các Enrollment còn nợ (phần này giữ)
            List<Enrollment> dues = s.createQuery(
                    "select e from Enrollment e " +
                    "join e.student st " +
                    "where st.id = :sid and e.status <> :cart " +
                    "and (coalesce(e.totalAmount,0) - coalesce(e.paidAmount,0)) > 0 " +
                    "order by e.updatedAt desc, e.id desc",
                    Enrollment.class
            )
            .setParameter("sid", studentId)
            .setParameter("cart", EnrollmentStatus.CART)
            .list();

            int dueCount = 0;
            BigDecimal dueSum = BigDecimal.ZERO;

            for (Enrollment e : dues) {
                BigDecimal due = e.getAmountDue();
                if (due == null || due.signum() <= 0) continue;

                dueCount++;
                dueSum = dueSum.add(due);

                LocalDateTime t = (e.getUpdatedAt() != null) ? e.getUpdatedAt() : e.getCreatedAt();

                rows.add(0, new TxRow(
                        t,
                        "THÔNG BÁO",
                        due,
                        "Bạn còn phải nộp " + vnd.format(due) + " VNĐ cho mã: " + nvl(e.getEnrollmentCode())
                ));
            }

            data.setAll(rows);

            if (dueCount > 0) {
                lblMsg.setText("🔔 Bạn có " + dueCount + " đơn chưa thanh toán, tổng còn nợ: " + vnd.format(dueSum) + " VNĐ");
                lblMsg.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:bold;");
            } else {
                lblMsg.setText("✅ Không có đơn nào cần nộp thêm.");
                lblMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
            }

        } catch (Exception e) {
            e.printStackTrace();
            setErr("Load ví/lịch sử lỗi: " + e.getMessage());
        }
    }

    private BigDecimal parseMoney(String text) {
        try {
            if (text == null) return null;
            String t = text.trim().replace(" ", "");
            if (t.isEmpty()) return null;
            // hỗ trợ 1.000.000 hoặc 1,000,000
            t = t.replace(".", "").replace(",", "");
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private void setOk(String msg) {
        lblMsg.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }

    private void setErr(String msg) {
        lblMsg.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        lblMsg.setText("❌ " + msg);
    }
}
