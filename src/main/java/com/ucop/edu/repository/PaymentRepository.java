package com.ucop.edu.repository;

import com.ucop.edu.entity.Payment;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class PaymentRepository {

    public List<Payment> findAllWithEnrollmentStudent() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select distinct p from Payment p " +
                            "left join fetch p.enrollment e " +
                            "left join fetch e.student " +
                            "order by p.id desc",
                    Payment.class
            ).list();
        }
    }

    public Payment findById(Long id, Session s) {
        return s.get(Payment.class, id);
    }

    public Payment findByIdWithEnrollmentStudent(Long id, Session s) {
        return s.createQuery(
                "select distinct p from Payment p " +
                        "left join fetch p.enrollment e " +
                        "left join fetch e.student " +
                        "where p.id = :id",
                Payment.class
        ).setParameter("id", id).uniqueResult();
    }
}
