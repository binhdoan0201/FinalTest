package com.ucop.edu.service.impl;

import com.ucop.edu.entity.*;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.entity.enums.PaymentStatus;
import com.ucop.edu.entity.enums.RefundStatus;
import com.ucop.edu.repository.PaymentRepository;
import com.ucop.edu.repository.RefundRepository;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepo = new PaymentRepository();
    private final RefundRepository refundRepo = new RefundRepository();
    private final WalletRepository walletRepo = new WalletRepository();

    public void createPayment(Long enrollmentId,
                              BigDecimal amount,
                              PaymentMethod method,
                              PaymentStatus status,
                              String transactionId) {

        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Enrollment e = s.get(Enrollment.class, enrollmentId);
            if (e == null) throw new IllegalStateException("Enrollment không tồn tại");

            Payment p = new Payment();
            p.setEnrollment(e);
            p.setAmount(amount);
            p.setPaymentMethod(method);
            p.setStatus(status == null ? PaymentStatus.PENDING : status);
            p.setTransactionId((transactionId == null || transactionId.isBlank())
                    ? genTxId()
                    : transactionId.trim());
            p.setCreatedAt(LocalDateTime.now());

            if (p.getStatus() == PaymentStatus.PAID) {
                p.setPaidAt(LocalDateTime.now());
                applyPaidEffects(s, e, p);
            }

            s.persist(p);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw ex;
        }
    }

    public void markPaid(Long paymentId) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Payment p = paymentRepo.findByIdWithEnrollmentStudent(paymentId, s);
            if (p == null) throw new IllegalStateException("Payment không tồn tại");
            if (p.getStatus() == PaymentStatus.PAID) throw new IllegalStateException("Payment đã PAID");

            Enrollment e = s.get(Enrollment.class, p.getEnrollment().getId());

            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(LocalDateTime.now());

            applyPaidEffects(s, e, p);

            s.merge(p);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw ex;
        }
    }

    public void requestRefund(Long paymentId, BigDecimal amount, String reason) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Payment p = paymentRepo.findById(paymentId, s);
            if (p == null) throw new IllegalStateException("Payment không tồn tại");

            if (amount.compareTo(p.getAmount()) > 0)
                throw new IllegalStateException("Refund > amount payment");

            Refund r = new Refund();
            r.setPayment(p);
            r.setAmount(amount);
            r.setReason(reason);
            r.setStatus(RefundStatus.REQUESTED);

            s.persist(r);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw ex;
        }
    }

    public void processRefund(Long refundId) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Refund r = refundRepo.findById(refundId, s);
            if (r == null) throw new IllegalStateException("Refund không tồn tại");
            if (r.getStatus() == RefundStatus.PROCESSED)
                throw new IllegalStateException("Refund đã PROCESSED");

            Payment p = paymentRepo.findByIdWithEnrollmentStudent(r.getPayment().getId(), s);
            Enrollment e = s.get(Enrollment.class, p.getEnrollment().getId());

            // WALLET: cộng lại ví
            if (p.getPaymentMethod() == PaymentMethod.WALLET && e.getStudent() != null) {
                Wallets w = walletRepo.getOrCreate(e.getStudent().getId(), s);
                w.setBalance(w.getBalance().add(r.getAmount()));
                s.merge(w);
            }

            // trừ paid_amount
            BigDecimal paid = (e.getPaidAmount() == null ? BigDecimal.ZERO : e.getPaidAmount());
            e.setPaidAmount(paid.subtract(r.getAmount()));
            if (e.getPaidAmount().signum() < 0) e.setPaidAmount(BigDecimal.ZERO);
            e.setUpdatedAt(LocalDateTime.now());
            e.setStatus("RMA_REQUESTED"); // hoặc "REFUNDED" tuỳ bạn

            // update status payment/refund
            r.setStatus(RefundStatus.PROCESSED);
            r.setProcessedAt(LocalDateTime.now());

            p.setStatus(PaymentStatus.REFUNDED);

            s.merge(e);
            s.merge(p);
            s.merge(r);

            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw ex;
        }
    }

    // ===================== PRIVATE =====================
    private String genTxId() {
        return "TX-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    // Khi PAID: tăng paid_amount + trừ wallet nếu WALLET
    private void applyPaidEffects(Session s, Enrollment e, Payment p) {
        if (e == null) return;

        BigDecimal paid = (e.getPaidAmount() == null ? BigDecimal.ZERO : e.getPaidAmount());
        BigDecimal total = (e.getTotalAmount() == null ? BigDecimal.ZERO : e.getTotalAmount());

        e.setPaidAmount(paid.add(p.getAmount()));
        e.setUpdatedAt(LocalDateTime.now());

        if (e.getPaidAmount().compareTo(total) >= 0) e.setStatus("PAID");
        else e.setStatus("PENDING_PAYMENT");

        // WALLET: trừ ví
        if (p.getPaymentMethod() == PaymentMethod.WALLET && e.getStudent() != null) {
            Wallets w = walletRepo.getOrCreate(e.getStudent().getId(), s);
            if (w.getBalance().compareTo(p.getAmount()) < 0) {
                throw new IllegalStateException("Ví không đủ tiền");
            }
            w.setBalance(w.getBalance().subtract(p.getAmount()));
            s.merge(w);
        }

        s.merge(e);
    }
}
