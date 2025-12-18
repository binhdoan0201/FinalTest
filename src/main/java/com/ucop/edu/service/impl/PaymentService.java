package com.ucop.edu.service.impl;

import com.ucop.edu.entity.*;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.entity.enums.PaymentStatus;
import com.ucop.edu.entity.enums.WalletTxType;
import com.ucop.edu.repository.PaymentRepository;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentService {

    private final WalletRepository walletRepo = new WalletRepository();
    private final PaymentRepository paymentRepo = new PaymentRepository();

    public Payment payOrder(Long studentId,
                            Long orderId,
                            BigDecimal amount,
                            PaymentMethod method,
                            String transactionIdOrNull) {

        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Order order = s.get(Order.class, orderId);
            if (order == null) throw new IllegalStateException("Không tìm thấy Order id=" + orderId);

            if (order.getStudent() == null || order.getStudent().getId() == null
                    || !order.getStudent().getId().equals(studentId)) {
                throw new IllegalStateException("Order không thuộc về bạn.");
            }

            BigDecimal total = nvl(order.getTotalAmount());
            BigDecimal paid = nvl(paymentRepo.sumPaidByOrder(s, orderId));
            BigDecimal due  = total.subtract(paid);
            if (due.signum() <= 0) throw new IllegalStateException("Đơn này đã thanh toán đủ.");

            if (amount == null || amount.signum() <= 0) amount = due; // để trống = trả hết
            if (amount.compareTo(due) > 0) throw new IllegalStateException("Số tiền vượt quá phần còn lại cần trả.");

            // =====================
            // PAYMENT record (pending, chờ duyệt)
            // =====================
            Payment p = new Payment();
            p.setOrder(order);
            p.setAmount(amount);
            p.setPaymentMethod(method);
            p.setStatus(PaymentStatus.PENDING);

            String txId = (transactionIdOrNull == null || transactionIdOrNull.trim().isEmpty())
                    ? ("TX-" + UUID.randomUUID())
                    : transactionIdOrNull.trim();
            p.setTransactionId(txId);
            s.persist(p);

            // gắn trạng thái order để staff biết đang chờ thanh toán
            order.setStatus("PENDING_PAYMENT");
            s.merge(order);

            tx.commit();
            return p;

        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("Thanh toán thất bại: " + e.getMessage(), e);
        }
    }

    public Payment approvePayment(Long paymentId, Long staffId, BigDecimal amountOverride) {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Payment payment = s.get(Payment.class, paymentId);
            if (payment == null) throw new IllegalStateException("Không tìm thấy payment id=" + paymentId);

            if (payment.getStatus() == PaymentStatus.PAID) throw new IllegalStateException("Payment đã ở trạng thái PAID.");
            if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELED) {
                throw new IllegalStateException("Payment đã bị từ chối.");
            }

            Order order = (payment.getOrder() == null) ? null : s.get(Order.class, payment.getOrder().getId());
            if (order == null) throw new IllegalStateException("Payment chưa gắn Order.");

            Account student = (order.getStudent() == null) ? null : s.get(Account.class, order.getStudent().getId());
            if (student == null) throw new IllegalStateException("Order chưa gắn Student.");

            BigDecimal total = nvl(order.getTotalAmount());
            BigDecimal paid = nvl(paymentRepo.sumPaidByOrder(s, order.getId()));
            BigDecimal due = total.subtract(paid);
            if (due.signum() <= 0) throw new IllegalStateException("Đơn này đã thanh toán đủ.");

            BigDecimal amountToPay = amountOverride;
            if (amountToPay == null || amountToPay.signum() <= 0) amountToPay = nvl(payment.getAmount());
            if (amountToPay.signum() <= 0) throw new IllegalStateException("Số tiền thanh toán không hợp lệ.");
            if (amountToPay.compareTo(due) > 0) throw new IllegalStateException("Số tiền vượt quá phần còn lại cần trả.");

            payment.setAmount(amountToPay);

            if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
                Wallets w = walletRepo.getOrCreate(student.getId(), s);
                w.sub(amountToPay);
                s.merge(w);

                WalletTransaction txw = new WalletTransaction();
                txw.setAccount(student);
                txw.setWallet(w);
                txw.setOrderId(order.getId());
                txw.setType(WalletTxType.PAYMENT_DEBIT);
                txw.setAmount(amountToPay);
                txw.setBalanceAfter(w.getBalance());
                String msg = "Duyệt payment#" + paymentId + " cho Order#" + order.getId();
                if (staffId != null) msg += " (staff#" + staffId + ")";
                txw.setMessage(msg);
                txw.setCreatedAt(LocalDateTime.now());
                s.persist(txw);
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            s.merge(payment);

            BigDecimal newPaid = paid.add(amountToPay);
            String oldStatus = order.getStatus();

            if (newPaid.compareTo(total) >= 0) {
                order.setStatus("PAID");
                if (!"PAID".equalsIgnoreCase(oldStatus)) {
                    consumePromotionIfAny(s, order, student);
                }
            } else {
                order.setStatus("PENDING_PAYMENT");
            }
            s.merge(order);

            tx.commit();
            return payment;

        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("Duyệt payment thất bại: " + e.getMessage(), e);
        }
    }

    public Payment rejectPayment(Long paymentId, Long staffId, String note) {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Payment payment = s.get(Payment.class, paymentId);
            if (payment == null) throw new IllegalStateException("Không tìm thấy payment id=" + paymentId);
            if (payment.getStatus() == PaymentStatus.PAID) throw new IllegalStateException("Payment đã được thanh toán.");

            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaidAt(null);

            // ghép note vào transactionId để dễ truy vết mà không cần đổi schema
            if (note != null && !note.trim().isEmpty()) {
                String prefix = "[REJECT";
                if (staffId != null) prefix += " staff#" + staffId;
                prefix += "] ";
                String txId = payment.getTransactionId();
                if (txId == null || txId.isBlank()) txId = prefix + note.trim();
                else txId = txId + " | " + prefix + note.trim();
                payment.setTransactionId(txId);
            }

            s.merge(payment);

            tx.commit();
            return payment;

        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("Từ chối payment thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Tiêu thụ voucher (tăng usedCount + ghi PromotionUsage) khi Order thanh toán đủ.
     * Nếu voucher đã hết lượt tại thời điểm pay => rollback payment và báo lỗi.
     */
    private void consumePromotionIfAny(Session s, Order order, Account student) {
        // tránh lazy lỗi: dùng query fetch enrollment + promotion
        Order mo = s.createQuery(
                        "select o from Order o " +
                                "left join fetch o.enrollment e " +
                                "left join fetch e.promotion " +
                                "where o.id = :oid", Order.class)
                .setParameter("oid", order.getId())
                .uniqueResult();

        if (mo == null) return;

        Enrollment en = mo.getEnrollment();
        if (en == null) return;

        Promotion promo = en.getPromotion();
        if (promo == null) return;

        Promotion managed = s.get(Promotion.class, promo.getId());
        if (managed == null) return;

        // validate date + remaining at "pay time"
        LocalDate today = LocalDate.now();
        if (managed.getValidFrom() != null && today.isBefore(managed.getValidFrom())) {
            throw new IllegalStateException("Voucher chưa đến ngày áp dụng. Hãy bỏ mã và thanh toán lại.");
        }
        if (managed.getValidTo() != null && today.isAfter(managed.getValidTo())) {
            throw new IllegalStateException("Voucher đã hết hạn. Hãy bỏ mã và thanh toán lại.");
        }

        int used = managed.getUsedCount() == null ? 0 : managed.getUsedCount();
        Integer max = managed.getMaxUsage();
        if (max != null && used >= max) {
            throw new IllegalStateException("Voucher đã hết lượt sử dụng. Hãy bỏ mã và thanh toán lại.");
        }

        // increment + log
        managed.setUsedCount(used + 1);
        s.merge(managed);

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(managed);
        usage.setUsedBy(student);
        usage.setUsedAt(LocalDateTime.now());
        s.persist(usage);
    }

    private BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
}
