package com.ucop.edu.repository;

import com.ucop.edu.entity.Course;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class CourseRepository {

    public List<Course> findAll() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            // join fetch category để tránh LazyInitializationException khi show table
            return s.createQuery(
                    "select c from Course c left join fetch c.category order by c.id asc",
                    Course.class
            ).list();
        }
    }

    public void save(Course c) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.persist(c);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void update(Course c) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            s.merge(c);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void deleteById(Long id) {
        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();
            Course c = s.get(Course.class, id);
            if (c != null) s.remove(c);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public List<Course> search(Long categoryId, String keyword) {
        String key = keyword == null ? "" : keyword.trim();

        String hql = "select c from Course c left join fetch c.category where 1=1 ";
        if (categoryId != null) hql += " and c.category.id = :catId ";
        if (!key.isEmpty()) hql += " and lower(c.name) like :kw ";
        hql += " order by c.id asc";

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Course> q = s.createQuery(hql, Course.class);
            if (categoryId != null) q.setParameter("catId", categoryId);
            if (!key.isEmpty()) q.setParameter("kw", "%" + key.toLowerCase() + "%");
            return q.list();
        }
    }
}
