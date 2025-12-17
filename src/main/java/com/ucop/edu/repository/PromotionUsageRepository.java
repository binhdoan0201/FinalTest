package com.ucop.edu.repository;

import com.ucop.edu.entity.PromotionUsage;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class PromotionUsageRepository {

    public PromotionUsage save(PromotionUsage usage) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.save(usage);
            tx.commit();
            return usage;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public long countByPromotion(Long promotionId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Long c = s.createQuery(
                            "SELECT count(u.id) FROM PromotionUsage u WHERE u.promotion.id = :pid",
                            Long.class)
                    .setParameter("pid", promotionId)
                    .uniqueResult();
            return c == null ? 0 : c;
        }
    }

    public long countByPromotionAndUser(Long promotionId, Long userId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Long c = s.createQuery(
                            "SELECT count(u.id) FROM PromotionUsage u WHERE u.promotion.id = :pid AND u.usedBy.id = :uid",
                            Long.class)
                    .setParameter("pid", promotionId)
                    .setParameter("uid", userId)
                    .uniqueResult();
            return c == null ? 0 : c;
        }
    }
}
