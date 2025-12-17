package com.ucop.edu.controller;

import com.ucop.edu.entity.*;
import com.ucop.edu.entity.enums.*;
import com.ucop.edu.repository.PaymentRepository;
import com.ucop.edu.repository.RefundRepository;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.util.HibernateUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.util.StringConverter;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class PaymentManagementController {

	// FILTER
	@FXML
	private ComboBox<PaymentStatus> cbFilterStatus;
	@FXML
	private ComboBox<PaymentMethod> cbFilterMethod;
	@FXML
	private TextField txtSearch;

	// PAYMENTS TABLE
	@FXML
	private TableView<Payment> tblPayments;
	@FXML
	private TableColumn<Payment, Long> colPayId;
	@FXML
	private TableColumn<Payment, String> colEnroll; // giữ fx:id cũ nhưng hiển thị ORDER
	@FXML
	private TableColumn<Payment, String> colStudent;
	@FXML
	private TableColumn<Payment, String> colAmount;
	@FXML
	private TableColumn<Payment, String> colMethod;
	@FXML
	private TableColumn<Payment, String> colStatus;
	@FXML
	private TableColumn<Payment, String> colPaidAt;

	// REFUNDS TABLE
	@FXML
	private TableView<Refund> tblRefunds;
	@FXML
	private TableColumn<Refund, Long> colRefundId;
	@FXML
	private TableColumn<Refund, String> colRefundAmount;
	@FXML
	private TableColumn<Refund, String> colRefundStatus;
	@FXML
	private TableColumn<Refund, String> colRefundAt;

	// FORM PAYMENT (Order)
	@FXML
	private ComboBox<Order> cbEnrollment; // giữ fx:id cũ cho khỏi sửa FXML, nhưng type là Order
	@FXML
	private TextField txtAmount;
	@FXML
	private ComboBox<PaymentMethod> cbMethod;
	@FXML
	private ComboBox<PaymentStatus> cbPayStatus;
	@FXML
	private TextField txtTransactionId;

	// FORM REFUND
	@FXML
	private TextField txtRefundAmount;
	@FXML
	private TextArea txtRefundReason;

	@FXML
	private Label lblMsg;

	private final PaymentRepository paymentRepo = new PaymentRepository();
	private final RefundRepository refundRepo = new RefundRepository();
	private final WalletRepository walletRepo = new WalletRepository();

	private final ObservableList<Payment> payments = FXCollections.observableArrayList();
	private final ObservableList<Refund> refunds = FXCollections.observableArrayList();

	private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@FXML
	public void initialize() {
		cbFilterStatus.setItems(FXCollections.observableArrayList(PaymentStatus.values()));
		cbFilterMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));

		cbMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
		cbPayStatus.setItems(FXCollections.observableArrayList(PaymentStatus.values()));
		cbMethod.setValue(PaymentMethod.WALLET);
		cbPayStatus.setValue(PaymentStatus.PAID);

		cbEnrollment.setConverter(new StringConverter<>() {
			@Override
			public String toString(Order o) {
				if (o == null)
					return "";
				String user = (o.getStudent() == null) ? "" : safe(o.getStudent().getUsername());
				return "#Order " + o.getId() + " - " + user + " - " + safe(o.getStatus());
			}

			@Override
			public Order fromString(String s) {
				return null;
			}
		});

		colPayId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));

		colEnroll.setCellValueFactory(c -> new SimpleStringProperty(
				c.getValue().getOrder() == null ? "" : ("#Order " + c.getValue().getOrder().getId())));

		colStudent.setCellValueFactory(c -> new SimpleStringProperty(
				(c.getValue().getOrder() == null || c.getValue().getOrder().getStudent() == null) ? ""
						: safe(c.getValue().getOrder().getStudent().getUsername())));

		colAmount.setCellValueFactory(c -> new SimpleStringProperty(vnd.format(nz(c.getValue().getAmount()))));
		colMethod.setCellValueFactory(c -> new SimpleStringProperty(
				c.getValue().getPaymentMethod() == null ? "" : c.getValue().getPaymentMethod().name()));
		colStatus.setCellValueFactory(
				c -> new SimpleStringProperty(c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()));
		colPaidAt.setCellValueFactory(c -> new SimpleStringProperty(
				c.getValue().getPaidAt() == null ? "" : dtf.format(c.getValue().getPaidAt())));

		tblPayments.setItems(payments);

		colRefundId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
		colRefundAmount.setCellValueFactory(c -> new SimpleStringProperty(vnd.format(nz(c.getValue().getAmount()))));
		colRefundStatus.setCellValueFactory(
				c -> new SimpleStringProperty(c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()));
		colRefundAt.setCellValueFactory(c -> new SimpleStringProperty(
				c.getValue().getProcessedAt() == null ? "" : dtf.format(c.getValue().getProcessedAt())));
		tblRefunds.setItems(refunds);

		tblPayments.getSelectionModel().selectedItemProperty().addListener((obs, old, cur) -> {
			if (cur != null)
				loadRefunds(cur.getId());
			else
				refunds.clear();
		});

		loadOrders();
		loadAll();
		info("ℹ Đã tải danh sách Payment (theo Order)");
	}

	@FXML
	private void backToDashboard(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.setTitle("UCOP - Admin Dashboard");
			stage.centerOnScreen();
		} catch (Exception e) {
			e.printStackTrace();
			err("Không quay lại được: " + e.getMessage());
		}
	}

	private void loadAll() {
		payments.setAll(paymentRepo.findAllFetchUI());
		refunds.clear();
	}

	private void loadRefunds(Long paymentId) {
		refunds.setAll(refundRepo.findByPaymentId(paymentId));
	}

	private void loadOrders() {
		try (Session s = HibernateUtil.getSessionFactory().openSession()) {
			var list = s.createQuery("select o from Order o join fetch o.student st "
					+ "where o.status in ('PENDING','PENDING_PAYMENT') " + "order by o.id desc", Order.class).list();
			cbEnrollment.setItems(FXCollections.observableArrayList(list));
			if (!list.isEmpty())
				cbEnrollment.setValue(list.get(0));
		}
	}

	@FXML
	private void handleFilter() {
		payments.setAll(
				paymentRepo.filterFetchUI(cbFilterStatus.getValue(), cbFilterMethod.getValue(), txtSearch.getText()));
		info("ℹ Đã lọc");
	}

	@FXML
	private void handleReset() {
		cbFilterStatus.setValue(null);
		cbFilterMethod.setValue(null);
		txtSearch.clear();
		loadAll();
		info("ℹ Đã reset");
	}

	@FXML
	private void handleCreatePayment() {
		Order o = cbEnrollment.getValue();
		if (o == null) {
			err("Chưa chọn Order");
			return;
		}

		BigDecimal amount = parseMoneySmart(txtAmount.getText());
		if (amount == null || amount.signum() <= 0) {
			err("Số tiền không hợp lệ");
			return;
		}

		PaymentMethod method = cbMethod.getValue();
		PaymentStatus status = cbPayStatus.getValue();
		if (method == null || status == null) {
			err("Chưa chọn method/status");
			return;
		}

		String txId = (txtTransactionId.getText() == null || txtTransactionId.getText().trim().isEmpty())
				? "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
				: txtTransactionId.getText().trim();

		Transaction tx = null;
		try (Session s = HibernateUtil.getSessionFactory().openSession()) {
			tx = s.beginTransaction();

			Order managedO = s.get(Order.class, o.getId());
			if (managedO == null)
				throw new IllegalStateException("Order không tồn tại");

			// ✅ PAID + WALLET: trừ ví + log
			if (status == PaymentStatus.PAID && method == PaymentMethod.WALLET) {
				Long studentId = managedO.getStudent().getId();
				Account student = s.get(Account.class, studentId);

				Wallets w = walletRepo.getOrCreate(studentId, s);
				BigDecimal bal = nz(w.getBalance());
				if (bal.compareTo(amount) < 0)
					throw new IllegalStateException("Ví không đủ. Hiện có " + bal + ", cần " + amount);

				w.setBalance(bal.subtract(amount));
				s.merge(w);

				WalletTransaction wt = new WalletTransaction();
				wt.setAccount(student);
				wt.setWallet(w);
				wt.setOrderId(managedO.getId()); // nếu entity bạn là orderId
				wt.setType(WalletTxType.PAYMENT_DEBIT);
				wt.setAmount(amount);
				wt.setBalanceAfter(nz(w.getBalance()));
				wt.setMessage("Admin tạo payment cho Order#" + managedO.getId());
				wt.setCreatedAt(LocalDateTime.now());
				s.persist(wt);
			}

			Payment p = new Payment();
			p.setOrder(managedO);
			p.setAmount(amount);
			p.setPaymentMethod(method);
			p.setStatus(status);
			p.setTransactionId(txId);
			if (status == PaymentStatus.PAID)
				p.setPaidAt(LocalDateTime.now());
			s.persist(p);

			// ✅ cập nhật Order + Enrollment theo NET PAID
			updateOrderAndEnrollmentByNetPaid(s, managedO);

			tx.commit();

			ok("✅ Tạo Payment thành công");
			loadOrders();
			loadAll();

		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			ex.printStackTrace();
			err("Tạo Payment lỗi: " + ex.getMessage());
		}
	}

	@FXML
	private void handleMarkPaid() {
		Payment p = tblPayments.getSelectionModel().getSelectedItem();
		if (p == null) {
			err("Chọn 1 Payment");
			return;
		}

		Transaction tx = null;
		try (Session s = HibernateUtil.getSessionFactory().openSession()) {
			tx = s.beginTransaction();

			Payment mp = s.get(Payment.class, p.getId());
			if (mp == null)
				throw new IllegalStateException("Payment không tồn tại");

			// ✅ FIX BUG: đã PAID thì rollback rồi return (không bỏ tx lơ lửng)
			if (mp.getStatus() == PaymentStatus.PAID) {
				if (tx != null)
					tx.rollback();
				info("ℹ Payment đã PAID");
				return;
			}

			Order o = (mp.getOrder() == null) ? null : s.get(Order.class, mp.getOrder().getId());
			if (o == null)
				throw new IllegalStateException("Payment chưa gắn Order (order_id null)");

			// ✅ WALLET: trừ ví + log
			if (mp.getPaymentMethod() == PaymentMethod.WALLET) {
				Long studentId = o.getStudent().getId();
				Account student = s.get(Account.class, studentId);
				BigDecimal amount = nz(mp.getAmount());

				Wallets w = walletRepo.getOrCreate(studentId, s);
				BigDecimal bal = nz(w.getBalance());
				if (bal.compareTo(amount) < 0)
					throw new IllegalStateException("Ví không đủ để mark paid. Hiện có " + bal + ", cần " + amount);

				w.setBalance(bal.subtract(amount));
				s.merge(w);

				WalletTransaction wt = new WalletTransaction();
				wt.setAccount(student);
				wt.setWallet(w);
				wt.setOrderId(o.getId());
				wt.setType(WalletTxType.PAYMENT_DEBIT);
				wt.setAmount(amount);
				wt.setBalanceAfter(nz(w.getBalance()));
				wt.setMessage("Admin mark PAID payment#" + mp.getId() + " Order#" + o.getId());
				wt.setCreatedAt(LocalDateTime.now());
				s.persist(wt);
			}

			mp.setStatus(PaymentStatus.PAID);
			mp.setPaidAt(LocalDateTime.now());
			s.merge(mp);

			updateOrderAndEnrollmentByNetPaid(s, o);

			tx.commit();
			ok("✅ Đã Mark PAID");
			loadOrders();
			loadAll();

		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			ex.printStackTrace();
			err("Mark PAID lỗi: " + ex.getMessage());
		}
	}

	@FXML
	private void handleRequestRefund() {
		Payment p = tblPayments.getSelectionModel().getSelectedItem();
		if (p == null) {
			err("Chọn Payment để hoàn");
			return;
		}

		BigDecimal amount = parseMoneySmart(txtRefundAmount.getText());
		if (amount == null)
			amount = nz(p.getAmount());
		if (amount.signum() <= 0) {
			err("RefundAmount không hợp lệ");
			return;
		}

		String reason = (txtRefundReason.getText() == null) ? "" : txtRefundReason.getText().trim();

		Transaction tx = null;
		try (Session s = HibernateUtil.getSessionFactory().openSession()) {
			tx = s.beginTransaction();

			Payment mp = s.get(Payment.class, p.getId());
			if (mp == null)
				throw new IllegalStateException("Payment không tồn tại");

			Refund r = new Refund();
			r.setPayment(mp);
			r.setAmount(amount);
			r.setReason(reason);
			r.setStatus(RefundStatus.REQUESTED);
			s.persist(r);

			tx.commit();
			ok("✅ Đã gửi yêu cầu hoàn");
			loadRefunds(p.getId());

		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			ex.printStackTrace();
			err("Request refund lỗi: " + ex.getMessage());
		}
	}

	@FXML
	private void handleApproveRefund() {
		updateRefundStatus(RefundStatus.APPROVED, "✅ Đã Approve");
	}

	@FXML
	private void handleRejectRefund() {
		updateRefundStatus(RefundStatus.REJECTED, "✅ Đã Reject");
	}

	private void updateRefundStatus(RefundStatus st, String okMsg) {
		Refund r = tblRefunds.getSelectionModel().getSelectedItem();
		if (r == null) {
			err("Chọn 1 Refund");
			return;
		}

		Transaction tx = null;
		try (Session s = HibernateUtil.getSessionFactory().openSession()) {
			tx = s.beginTransaction();

			Refund mr = s.get(Refund.class, r.getId());
			if (mr == null)
				throw new IllegalStateException("Refund không tồn tại");

			mr.setStatus(st);
			s.merge(mr);

			tx.commit();
			ok(okMsg);

			Payment p = tblPayments.getSelectionModel().getSelectedItem();
			if (p != null)
				loadRefunds(p.getId());

		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			ex.printStackTrace();
			err("Update refund lỗi: " + ex.getMessage());
		}
	}

	@FXML
	private void handleProcessRefund() {
		Refund r = tblRefunds.getSelectionModel().getSelectedItem();
		if (r == null) {
			err("Chọn 1 Refund");
			return;
		}

		Transaction tx = null;
		try (Session s = HibernateUtil.getSessionFactory().openSession()) {
			tx = s.beginTransaction();

			Refund mr = s.get(Refund.class, r.getId());
			if (mr == null)
				throw new IllegalStateException("Refund không tồn tại");
			if (mr.getStatus() != RefundStatus.APPROVED)
				throw new IllegalStateException("Chỉ Process khi APPROVED");

			Payment p = s.get(Payment.class, mr.getPayment().getId());
			if (p == null)
				throw new IllegalStateException("Payment không tồn tại");

			Order o = (p.getOrder() == null) ? null : s.get(Order.class, p.getOrder().getId());
			if (o == null)
				throw new IllegalStateException("Payment chưa gắn Order (order_id null)");

			BigDecimal refundAmount = nz(mr.getAmount());

			// ✅ WALLET => cộng ví + log
			if (p.getPaymentMethod() == PaymentMethod.WALLET) {
				Long studentId = o.getStudent().getId();
				Account student = s.get(Account.class, studentId);

				Wallets w = walletRepo.getOrCreate(studentId, s);
				w.setBalance(nz(w.getBalance()).add(refundAmount));
				s.merge(w);

				WalletTransaction wt = new WalletTransaction();
				wt.setAccount(student);
				wt.setWallet(w);
				wt.setOrderId(o.getId());
				wt.setType(WalletTxType.REFUND_CREDIT);
				wt.setAmount(refundAmount);
				wt.setBalanceAfter(nz(w.getBalance()));
				wt.setMessage("Refund processed for Order#" + o.getId() + " (payment#" + p.getId() + ")");
				wt.setCreatedAt(LocalDateTime.now());
				s.persist(wt);
			}

			mr.setStatus(RefundStatus.PROCESSED);
			mr.setProcessedAt(LocalDateTime.now());
			s.merge(mr);

			// ✅ cập nhật Order + Enrollment theo NET PAID
			updateOrderAndEnrollmentByNetPaid(s, o);

			tx.commit();
			ok("✅ Đã Process hoàn tiền");

			Payment selected = tblPayments.getSelectionModel().getSelectedItem();
			if (selected != null)
				loadRefunds(selected.getId());
			loadOrders();
			loadAll();

		} catch (Exception ex) {
			if (tx != null)
				tx.rollback();
			ex.printStackTrace();
			err("Process lỗi: " + ex.getMessage());
		}
	}

	@FXML
	private void handleClearForm() {
		txtAmount.clear();
		txtTransactionId.clear();
		txtRefundAmount.clear();
		txtRefundReason.clear();
		info("ℹ Đã làm mới form");
	}

	// =========================================================
	// ✅ CORE: Update Order.status + Enrollment.paidAmount theo NET PAID
	// - total lấy từ Enrollment.totalAmount (đúng schema DB bạn gửi)
	// - netPaid = sum(Payment PAID) - sum(Refund PROCESSED)
	// =========================================================
	private void updateOrderAndEnrollmentByNetPaid(Session s, Order o) {
		if (o == null || o.getId() == null)
			return;

		// total lấy từ enrollment (ổn định nhất với schema của bạn)
		Enrollment e = o.getEnrollment() != null ? s.get(Enrollment.class, o.getEnrollment().getId()) : null;
		BigDecimal total = (e == null) ? BigDecimal.ZERO : nz(e.getTotalAmount());

		BigDecimal paid = (BigDecimal) s
				.createQuery("select coalesce(sum(p.amount),0) from Payment p "
						+ "where p.order.id = :oid and p.status = :st")
				.setParameter("oid", o.getId()).setParameter("st", PaymentStatus.PAID).uniqueResult();

		BigDecimal refunded = (BigDecimal) s
				.createQuery("select coalesce(sum(r.amount),0) from Refund r " + "join r.payment p "
						+ "where p.order.id = :oid and r.status = :rst")
				.setParameter("oid", o.getId()).setParameter("rst", RefundStatus.PROCESSED).uniqueResult();

		BigDecimal netPaid = nz(paid).subtract(nz(refunded));
		if (netPaid.signum() < 0)
			netPaid = BigDecimal.ZERO;

		BigDecimal due = total.subtract(netPaid);
		if (due.signum() <= 0)
			o.setStatus("PAID");
		else
			o.setStatus("PENDING_PAYMENT");

		s.merge(o);

		// ✅ đồng bộ paidAmount vào enrollment để các màn Student/Payment hiển thị “còn
		// nợ” đúng
		if (e != null) {
			e.setPaidAmount(netPaid);
			s.merge(e);
		}
	}

	// ====== Helpers ======
	private BigDecimal parseMoneySmart(String text) {
		try {
			if (text == null)
				return null;
			String t = text.trim();
			if (t.isEmpty())
				return null;

			t = t.replace(" ", "");

			// hỗ trợ 1.000.000 hoặc 1,000,000
			if (t.contains(".") && t.indexOf('.') != t.lastIndexOf('.'))
				t = t.replace(".", "");
			t = t.replace(",", "");

			if (t.isEmpty())
				return null;
			return new BigDecimal(t);
		} catch (Exception e) {
			return null;
		}
	}

	private BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private String safe(String s) {
		return s == null ? "" : s;
	}

	private void ok(String msg) {
		lblMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
		lblMsg.setText(msg);
	}

	private void err(String msg) {
		lblMsg.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:bold;");
		lblMsg.setText("❌ " + msg);
	}

	private void info(String msg) {
		lblMsg.setStyle("-fx-text-fill:#2563eb; -fx-font-weight:bold;");
		lblMsg.setText(msg);
	}
}
