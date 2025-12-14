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
            order.setTotalAmount(BigDecimal.ZERO);

            BigDecimal totalAmount = BigDecimal.ZERO;

            // 5) create order items
            for (CartItem ci : cartItems) {
                int quantity = (ci.getQuantity() == null ? 0 : ci.getQuantity());
                if (quantity <= 0) continue;

                BigDecimal price = ci.getPriceAtAdd();
                if (price == null) price = BigDecimal.ZERO;

                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));

                OrderItem oi = new OrderItem();
                oi.setCourse(ci.getCourse());
                oi.setQuantity(quantity);

                // ✅ bắt buộc set đủ các cột NOT NULL đang có trong DB
                // - unit_price (nếu DB có)
                oi.setUnitPrice(price);

                // - price_at_purchase (DB bạn đang báo NOT NULL)
                oi.setPriceAtPurchase(price);

                // - line_total (DB bạn đang báo NOT NULL)
                oi.setLineTotal(lineTotal);

                // ✅ set quan hệ 2 chiều
                order.addItem(oi);

                totalAmount = totalAmount.add(lineTotal);
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
            } catch (Exception ignore) {}

            throw new RuntimeException("Checkout thất bại: " + e.getMessage(), e);
        }
    }
}
