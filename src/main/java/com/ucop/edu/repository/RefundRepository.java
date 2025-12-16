package com.ucop.edu.repository;

import com.ucop.edu.entity.Refund;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class RefundRepository {

    public List<Refund> findByPaymentId(Long paymentId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select r from Refund r " +
                            "join fetch r.payment p " +
                            "where p.id = :pid " +
                            "order by r.id desc",
                    Refund.class
            ).setParameter("pid", paymentId).list();
        }
    }

    public Refund findById(Long refundId, Session s) {
        return s.get(Refund.class, refundId);
    }
}
