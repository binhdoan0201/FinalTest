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

            // 1) get student
            Account student = session.get(Account.class, studentId);
            if (student == null) {
                throw new IllegalStateException("Không tìm thấy student id=" + studentId);
            }

            // 2) get cart
            Cart cart = session.createQuery(
                            "FROM Cart c WHERE c.student.id = :sid", Cart.class)
                    .setParameter("sid", studentId)
                    .uniqueResult();

            if (cart == null) {
                throw new IllegalStateException("Giỏ hàng trống (chưa có cart).");
            }

            // 3) get cart items
            List<CartItem> cartItems = session.createQuery(
                            "FROM CartItem ci WHERE ci.cart.id = :cid", CartItem.class)
                    .setParameter("cid", cart.getId())
                    .list();

            if (cartItems == null || cartItems.isEmpty()) {
                throw new IllegalStateException("Giỏ hàng trống.");
            }

            // 4) create order
            Order order = new Order();
            order.setStudent(student);
            order.setCreatedAt(LocalDateTime.now());
            order.setStatus("PENDING");

            BigDecimal totalAmount = BigDecimal.ZERO;

            // 5) create order items + TRỪ GHẾ
            for (CartItem ci : cartItems) {
                int quantity = (ci.getQuantity() == null ? 0 : ci.getQuantity());
                if (quantity <= 0) continue;

                // lấy course từ session cho chắc (tránh proxy/dirty)
                Course course = session.get(Course.class, ci.getCourse().getId());
                if (course == null) continue;

                // ✅ trừ available_seats để demo "đã đăng ký" nhảy
                int available = (course.getAvailableSeats() == null ? 0 : course.getAvailableSeats());
                if (available < quantity) {
                    throw new IllegalStateException("Khóa học '" + course.getName() + "' không đủ chỗ! Còn " + available);
                }
                course.setAvailableSeats(available - quantity);
                session.merge(course);

                BigDecimal price = ci.getPriceAtAdd();
                if (price == null) price = BigDecimal.ZERO;

                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                totalAmount = totalAmount.add(lineTotal);

                OrderItem oi = new OrderItem();
                oi.setCourse(course);
                oi.setQuantity(quantity);

                // ✅ set đủ các cột NOT NULL trong DB order_items
                oi.setUnitPrice(price);
                oi.setPriceAtPurchase(price);
                oi.setLineTotal(lineTotal);

                // set quan hệ 2 chiều
                order.addItem(oi);
            }

            if (order.getItems().isEmpty()) {
                throw new IllegalStateException("Giỏ hàng không hợp lệ (quantity <= 0).");
            }

            order.setTotalAmount(totalAmount);

            // 6) persist order (cascade persist items)
            session.persist(order);

            // 7) clear cart items
            session.createQuery("DELETE FROM CartItem ci WHERE ci.cart.id = :cid")
                    .setParameter("cid", cart.getId())
                    .executeUpdate();

            tx.commit();
            return order;

        } catch (Exception e) {
            try {
                if (tx != null && tx.isActive()) tx.rollback();
            } catch (Exception ignore) { }

            throw new RuntimeException("Checkout thất bại: " + e.getMessage(), e);
        }
    }
}
