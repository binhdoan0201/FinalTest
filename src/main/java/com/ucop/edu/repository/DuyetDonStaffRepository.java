package com.ucop.edu.repository;

import com.ucop.edu.entity.Order;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

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
}
