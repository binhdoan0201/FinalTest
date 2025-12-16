package com.ucop.edu.service.impl;

import com.ucop.edu.entity.*;
import com.ucop.edu.entity.enums.*;
import com.ucop.edu.repository.ReturnRequestRepository;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReturnRequestService {

    private final ReturnRequestRepository repo = new ReturnRequestRepository();
    private final WalletRepository walletRepo = new WalletRepository();

    // ✅ STUDENT: create theo ORDER để “giống thanh toán”
    public ReturnRequest createByOrder(Long studentId, Long orderId, ReturnRequestType type,
                                       BigDecimal refundAmountOrNull, String reason) {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Order o = s.get(Order.class, orderId);
            if (o == null) throw new IllegalStateException("Order không tồn tại");

            Account st = o.getStudent();
            if (st == null || st.getId() == null || !st.getId().equals(studentId))
                throw new IllegalStateException("Order không thuộc về bạn");

            Enrollment e = o.getEnrollment();
            if (e == null) throw new IllegalStateException("Order chưa gắn Enrollment");

            if (type == null) type = ReturnRequestType.REFUND;

            // chặn ticket active theo enrollment
            Long active = repo.countActiveByEnrollment(s, e.getId());
            if (active != null && active > 0)
                throw new IllegalStateException("Đã có yêu cầu đang chờ xử lý cho đơn này");

            // refund chỉ khi đã trả > 0
            if (type == ReturnRequestType.REFUND) {
                BigDecimal paid = nz(e.getPaidAmount());
                if (paid.signum() <= 0)
                    throw new IllegalStateException("Chưa có thanh toán, không thể hoàn");

                if (refundAmountOrNull != null && refundAmountOrNull.compareTo(paid) > 0)
                    throw new IllegalStateException("Số tiền hoàn vượt quá đã trả: " + paid);
            }

            ReturnRequest rr = new ReturnRequest();
            rr.setOrder(o);
            rr.setEnrollment(e);
            rr.setStudent(st);
            rr.setType(type);
            rr.setRefundAmount(refundAmountOrNull); // null = FULL/AUTO
            rr.setReason(reason);

            s.persist(rr);

            tx.commit();
            return rr;

        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Gửi yêu cầu thất bại: " + ex.getMessage(), ex);
        }
    }

    // ===== STAFF: list =====
    public List<ReturnRequest> listRequested() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return repo.findRequestedFetchUI(s);
        }
    }

    public void reject(Long requestId, String staffNote) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            ReturnRequest rr = repo.findByIdFetchUI(s, requestId);
            if (rr == null) throw new IllegalStateException("ReturnRequest không tồn tại");
            if (rr.getStatus() != ReturnRequestStatus.REQUESTED)
                throw new IllegalStateException("Ticket không còn REQUESTED");

            rr.setStatus(ReturnRequestStatus.REJECTED);
            rr.setProcessedAt(LocalDateTime.now());
            rr.setStaffNote(staffNote);

            s.merge(rr);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Reject thất bại: " + ex.getMessage(), ex);
        }
    }

    public void approve(Long requestId, BigDecimal approveAmountOrNull, String staffNote) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            ReturnRequest rr = repo.findByIdFetchUI(s, requestId);
            if (rr == null) throw new IllegalStateException("ReturnRequest không tồn tại");
            if (rr.getStatus() != ReturnRequestStatus.REQUESTED)
                throw new IllegalStateException("Ticket không còn REQUESTED");

            Enrollment e = rr.getEnrollment();
            BigDecimal paid = nz(e.getPaidAmount());
            if (paid.signum() <= 0) throw new IllegalStateException("Enrollment chưa có paidAmount");

            BigDecimal amount =
                    approveAmountOrNull != null ? approveAmountOrNull :
                    (rr.getRefundAmount() != null ? rr.getRefundAmount() : paid);

            if (amount.signum() <= 0) throw new IllegalStateException("RefundAmount phải > 0");
            if (amount.compareTo(paid) > 0) throw new IllegalStateException("Hoàn vượt quá đã trả: " + paid);

            rr.setRefundAmount(amount);
            rr.setStatus(ReturnRequestStatus.APPROVED);
            rr.setProcessedAt(LocalDateTime.now());
            rr.setStaffNote(staffNote);

            s.merge(rr);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Approve thất bại: " + ex.getMessage(), ex);
        }
    }

    public void processToWallet(Long requestId) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            ReturnRequest rr = repo.findByIdFetchUI(s, requestId);
            if (rr == null) throw new IllegalStateException("ReturnRequest không tồn tại");
            if (rr.getStatus() != ReturnRequestStatus.APPROVED)
                throw new IllegalStateException("Chỉ Process khi ticket ở APPROVED");

            Enrollment e = rr.getEnrollment();
            Account student = rr.getStudent();

            BigDecimal amount = rr.getRefundAmount();
            if (amount == null || amount.signum() <= 0)
                throw new IllegalStateException("RefundAmount chưa hợp lệ");

            Wallets w = walletRepo.getOrCreate(student.getId(), s);
            w.setBalance(nz(w.getBalance()).add(amount));
            s.merge(w);

            WalletTransaction wt = new WalletTransaction();
            wt.setAccount(student);
            wt.setWallet(w);
            wt.setType(WalletTxType.REFUND_CREDIT);
            wt.setAmount(amount);
            wt.setBalanceAfter(nz(w.getBalance()));
            wt.setMessage("Refund for Order#" + (rr.getOrder() != null ? rr.getOrder().getId() : "?") +
                    " / Enrollment#" + e.getId() + " (RR#" + rr.getId() + ")");
            wt.setCreatedAt(LocalDateTime.now());
            s.persist(wt);

            Refund refund = new Refund();
            refund.setPayment(rr.getPayment()); // có thể null
            refund.setAmount(amount);
            refund.setReason("From ReturnRequest#" + rr.getId() + " | " + (rr.getReason() == null ? "" : rr.getReason()));
            refund.setStatus(RefundStatus.PROCESSED);
            refund.setProcessedAt(LocalDateTime.now());
            s.persist(refund);

            rr.setStatus(ReturnRequestStatus.PROCESSED);
            rr.setProcessedAt(LocalDateTime.now());
            s.merge(rr);

            tx.commit();
        } catch (Exception ex) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Process thất bại: " + ex.getMessage(), ex);
        }
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
