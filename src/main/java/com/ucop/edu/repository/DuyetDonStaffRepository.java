package com.ucop.edu.repository;

import com.ucop.edu.entity.Order;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.ucop.edu.entity.Wallets;
import org.hibernate.LockMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.LockModeType;
public class DuyetDonStaffRepository {

    public List<Order> findByStatusWithStudent(String status) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            // JOIN FETCH để lấy student (LAZY) tránh LazyInitialization khi lên UI
            return s.createQuery(
                            "select distinct o " +
                                    "from Order o " +
                                    "join fetch o.student st " +
                                    "where o.status = :st " +
                                    "order by o.createdAt desc",
                            Order.class
                    )
                    .setParameter("st", status)
                    .getResultList();
        }
    }

    public boolean updateStatus(Long orderId, String newStatus) {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Order o = s.get(Order.class, orderId);
            if (o == null) return false;

            o.setStatus(newStatus); // field status trong entity
            s.merge(o);

            tx.commit();
            return true;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public int updateAllPendingToSuccess() {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            int updated = s.createMutationQuery(
                            "update Order o set o.status = :newStatus where o.status = :oldStatus"
                    )
                    .setParameter("newStatus", "SUCCESS")
                    .setParameter("oldStatus", "PENDING")
                    .executeUpdate();

            tx.commit();
            return updated;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
    
    public int updateAllPaidToRefund() {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            int updated = s.createMutationQuery(
                            "update Order o set o.status = :newStatus where o.status = :oldStatus"
                    )
                    .setParameter("newStatus", "REFUND")
                    .setParameter("oldStatus", "REFUND_PENDING")
                    .executeUpdate();

            tx.commit();
            return updated;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
    
    public boolean refundPaidOrderToWallet(Long orderId) {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            // Load order + student (tránh lazy)
            Order o = s.createQuery(
                            "select o from Order o " +
                            "join fetch o.student st " +
                            "where o.id = :id",
                            Order.class
                    )
                    .setParameter("id", orderId)
                    .uniqueResult();

            if (o == null) return false;

            if (!"REFUND_PENDING".equalsIgnoreCase(o.getStatus())) {
                throw new RuntimeException("Chỉ refund khi Order đang REFUND_PENDING!");
            }

            Long studentId = (o.getStudent() == null) ? null : o.getStudent().getId();
            if (studentId == null) throw new RuntimeException("Order không có student!");

            // Lấy ví theo account_id
            Wallets w = s.createQuery(
                            "select w from Wallets w where w.account.id = :aid",
                            Wallets.class
                    )
                    .setParameter("aid", studentId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE) // chống cộng tiền trùng khi bấm 2 lần
                    .uniqueResult();

            if (w == null) {
                throw new RuntimeException("Student chưa có ví (wallets)!");
            }

            BigDecimal amount = o.getTotalAmount();
            if (amount == null) amount = BigDecimal.ZERO;

            // + tiền vào ví
            w.add(amount);

            // đổi status
            o.setStatus("REFUND");

            s.merge(w);
            s.merge(o);

            tx.commit();
            return true;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
    
    public int refundAllPaidToWallet() {
        Transaction tx = null;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            List<Order> paidOrders = s.createQuery(
                            "select o from Order o " +
                            "join fetch o.student st " +
                            "where o.status = :st " +
                            "order by o.createdAt desc",
                            Order.class
                    )
                    .setParameter("st", "REFUND_PENDING")
                    .getResultList();

            int updated = 0;

            for (Order o : paidOrders) {
                Long studentId = (o.getStudent() == null) ? null : o.getStudent().getId();
                if (studentId == null) continue;

                Wallets w = s.createQuery(
                                "select w from Wallets w where w.account.id = :aid",
                                Wallets.class
                        )
                        .setParameter("aid", studentId)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .uniqueResult();

                if (w == null) continue;

                BigDecimal amount = o.getTotalAmount();
                if (amount == null) amount = BigDecimal.ZERO;

                w.add(amount);
                o.setStatus("REFUND");

                s.merge(w);
                s.merge(o);
                updated++;
            }

            tx.commit();
            return updated;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
