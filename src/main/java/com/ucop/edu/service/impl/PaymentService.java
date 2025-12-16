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
            BigDecimal paid = paymentRepo.sumPaidByOrder(s, orderId);
            BigDecimal due  = total.subtract(paid);
            if (due.signum() <= 0) throw new IllegalStateException("Đơn này đã thanh toán đủ.");

            if (amount == null || amount.signum() <= 0) amount = due; // để trống = trả hết
            if (amount.compareTo(due) > 0) throw new IllegalStateException("Số tiền vượt quá phần còn lại cần trả.");

            // WALLET
            Account student = s.get(Account.class, studentId);
            Wallets w = walletRepo.getOrCreate(studentId, s);

            // trừ ví
            w.sub(amount);
            s.merge(w);

            // log transaction (✅ set object wallet + account, KHÔNG set id)
            WalletTransaction txw = new WalletTransaction();
            txw.setAccount(student);
            txw.setWallet(w);
            txw.setOrderId(orderId); // ✅ DB bạn có order_id
            txw.setType(WalletTxType.PAYMENT_DEBIT);
            txw.setAmount(amount);
            txw.setBalanceAfter(w.getBalance());
            txw.setMessage("Thanh toán đơn #" + orderId);
            txw.setCreatedAt(LocalDateTime.now());
            s.persist(txw);




            // create payment
            Payment p = new Payment();
            p.setOrder(order);
            p.setAmount(amount);
            p.setPaymentMethod(method);
            p.setStatus(PaymentStatus.PAID);

            String txId = (transactionIdOrNull == null || transactionIdOrNull.trim().isEmpty())
                    ? ("TX-" + UUID.randomUUID())
                    : transactionIdOrNull.trim();
            p.setTransactionId(txId);
            p.setPaidAt(LocalDateTime.now());

            s.persist(p);

            // update order status
            BigDecimal newPaid = paid.add(amount);
            if (newPaid.compareTo(total) >= 0) {
                order.setStatus("PAID");
            } else {
                order.setStatus("PENDING_PAYMENT");
            }
            s.merge(order);

            tx.commit();
            return p;

        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("Thanh toán thất bại: " + e.getMessage(), e);
        }
    }

    private BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
}
