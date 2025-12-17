package com.ucop.edu.repository;

import com.ucop.edu.entity.Promotion;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PromotionRepository {

    public Promotion findById(Long id) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.get(Promotion.class, id);
        }
    }

    public Promotion findByCode(String code) {
        if (code == null) return null;
        String c = code.trim().toUpperCase();
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery("select p from Promotion p where upper(p.code)=:c", Promotion.class)
                    .setParameter("c", c)
                    .uniqueResult();
        }
    }

    public List<Promotion> findAll() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            // ❗ KHÔNG order by createdAt vì entity Promotion bạn không có field đó.
            return s.createQuery("select p from Promotion p order by p.id desc", Promotion.class).list();
        }
    }

    public Promotion saveOrUpdate(Promotion p) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Promotion merged = (Promotion) s.merge(p);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Promotion p = s.get(Promotion.class, id);
            if (p != null) s.remove(p);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
