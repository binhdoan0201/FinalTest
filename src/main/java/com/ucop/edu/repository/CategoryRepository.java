package com.ucop.edu.repository;

import com.ucop.edu.entity.Categories;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CategoryRepository {

    public List<Categories> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Categories c order by c.id desc", Categories.class).list();
        }
    }

    public List<Categories> findRoot() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Categories c where c.parent is null order by c.name", Categories.class).list();
        }
    }

    public long countChildren(Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long cnt = session.createQuery(
                    "select count(c.id) from Categories c where c.parent.id = :id", Long.class)
                .setParameter("id", categoryId)
                .uniqueResult();
            return cnt == null ? 0 : cnt;
        }
    }

    public long countCourses(Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Course entity phải có field: category (ManyToOne)
            Long cnt = session.createQuery(
                    "select count(co.id) from Course co where co.category.id = :id", Long.class)
                .setParameter("id", categoryId)
                .uniqueResult();
            return cnt == null ? 0 : cnt;
        }
    }

    public void save(Categories c) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(c);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void update(Categories c) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(c);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void delete(Categories c) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.remove(session.contains(c) ? c : session.merge(c));
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
