package com.ucop.edu.repository;

import com.ucop.edu.entity.Payment;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.entity.enums.PaymentStatus;
import com.ucop.edu.entity.enums.RefundStatus;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {

    // ================== ADMIN UI ==================

    // Load tất cả payment cho admin (fetch order + student)
    public List<Payment> findAllFetchUI() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select p from Payment p " +
                    "join fetch p.order o " +
                    "join fetch o.student st " +
                    "order by p.id desc",
                    Payment.class
            ).list();
        }
    }

    // Lọc payment cho admin
    public List<Payment> filterFetchUI(PaymentStatus st, PaymentMethod method, String keyword) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            StringBuilder hql = new StringBuilder(
                    "select p from Payment p " +
                    "join fetch p.order o " +
                    "join fetch o.student st " +
                    "where 1=1 "
            );

            List<String> conditions = new ArrayList<>();

            if (st != null) {
                hql.append(" and p.status = :st ");
            }
            if (method != null) {
                hql.append(" and p.paymentMethod = :m ");
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                hql.append(" and (")
                        .append(" lower(st.username) like :kw ")
                        .append(" or lower(o.status) like :kw ")
                        .append(" or cast(o.id as string) like :kw ")
                        .append(" or lower(coalesce(p.transactionId,'')) like :kw ")
                        .append(" ) ");
            }

            hql.append(" order by p.id desc ");

            Query<Payment> q = s.createQuery(hql.toString(), Payment.class);

            if (st != null) q.setParameter("st", st);
            if (method != null) q.setParameter("m", method);
            if (keyword != null && !keyword.trim().isEmpty()) {
                q.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
            }

            return q.list();
        }
    }

    // ================== STUDENT UI ==================

    // Lịch sử thanh toán của student (fetch order + student)
    public List<Payment> findByStudentFetchUI(Long studentId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select p from Payment p " +
                    "join fetch p.order o " +
                    "join fetch o.student st " +
                    "where st.id = :sid " +
                    "order by p.id desc",
                    Payment.class
            ).setParameter("sid", studentId).list();
        }
    }

    // ================== SUM HELPERS (Order chuẩn) ==================

    // Tổng tiền PAID theo Order
    public BigDecimal sumPaidByOrder(Session s, Long orderId) {
        Query<BigDecimal> q = s.createQuery(
                "select coalesce(sum(p.amount),0) " +
                "from Payment p " +
                "where p.order.id = :oid and p.status = :st",
                BigDecimal.class
        );
        q.setParameter("oid", orderId);
        q.setParameter("st", PaymentStatus.PAID);
        return q.uniqueResult();
    }

    // Tổng tiền Refund đã PROCESSED theo Order
    public BigDecimal sumRefundProcessedByOrder(Session s, Long orderId) {
        Query<BigDecimal> q = s.createQuery(
                "select coalesce(sum(r.amount),0) " +
                "from Refund r " +
                "join r.payment p " +
                "where p.order.id = :oid and r.status = :rst",
                BigDecimal.class
        );
        q.setParameter("oid", orderId);
        q.setParameter("rst", RefundStatus.PROCESSED);
        return q.uniqueResult();
    }
}
