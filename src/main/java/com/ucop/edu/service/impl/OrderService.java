package com.ucop.edu.service.impl;

import com.ucop.edu.entity.*;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderService {

    public Order checkout(Long studentId) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Account student = session.get(Account.class, studentId);
            if (student == null) {
                throw new IllegalStateException("Không tìm thấy student id=" + studentId);
            }

            Cart cart = session.createQuery("from Cart c where c.student.id = :sid", Cart.class)
                    .setParameter("sid", studentId)
                    .uniqueResult();

            if (cart == null) throw new IllegalStateException("Giỏ hàng trống (chưa có cart).");

            List<CartItem> cartItems = session.createQuery(
                            "from CartItem ci where ci.cart.id = :cid", CartItem.class)
                    .setParameter("cid", cart.getId())
                    .list();

            if (cartItems == null || cartItems.isEmpty()) throw new IllegalStateException("Giỏ hàng trống.");

            Order order = new Order();
            order.setStudent(student);
            order.setCreatedAt(LocalDateTime.now());
            order.setStatus("PENDING_PAYMENT"); // ✅ chuẩn để thanh toán bám vào

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CartItem ci : cartItems) {
                int quantity = (ci.getQuantity() == null ? 0 : ci.getQuantity());
                if (quantity <= 0) continue;

                Course course = session.get(Course.class, ci.getCourse().getId());
                if (course == null) continue;

                int available = (course.getAvailableSeats() == null ? 0 : course.getAvailableSeats());
                if (available < quantity) {
                    throw new IllegalStateException("Khóa học '" + course.getName() + "' không đủ chỗ! Còn " + available);
                }
                course.setAvailableSeats(available - quantity);
                session.merge(course);

                BigDecimal price = (ci.getPriceAtAdd() == null ? BigDecimal.ZERO : ci.getPriceAtAdd());
                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                totalAmount = totalAmount.add(lineTotal);

                OrderItem oi = new OrderItem();
                oi.setCourse(course);
                oi.setQuantity(quantity);
                oi.setUnitPrice(price);
                oi.setPriceAtPurchase(price);
                oi.setLineTotal(lineTotal);

                order.addItem(oi);
            }

            if (order.getItems().isEmpty()) {
                throw new IllegalStateException("Giỏ hàng không hợp lệ (quantity <= 0).");
            }

            order.setTotalAmount(totalAmount);
            session.persist(order);

            session.createQuery("delete from CartItem ci where ci.cart.id = :cid")
                    .setParameter("cid", cart.getId())
                    .executeUpdate();

            tx.commit();
            return order;

        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            throw new RuntimeException("Checkout thất bại: " + e.getMessage(), e);
        }
    }
}
