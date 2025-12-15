package com.ucop.edu.repository;

import com.ucop.edu.entity.Enrollment;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class EnrollmentRepository {

    public List<Enrollment> findAllWithStudent() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select distinct e from Enrollment e " +
                            "left join fetch e.student " +
                            "order by e.id desc",
                    Enrollment.class
            ).list();
        }
    }

    public Enrollment findById(Long id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.get(Enrollment.class, id);
        }
    }
}
