package com.ucop.edu.service.impl;

import com.ucop.edu.entity.*;
import com.ucop.edu.entity.enums.EnrollmentStatus;
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
            Cart cart = session.createQuery("from Cart c where c.student.id = :sid", Cart.class)
                    .setParameter("sid", studentId)
                    .uniqueResult();

            if (cart == null) throw new IllegalStateException("Giỏ hàng trống (chưa có cart).");

            // 3) get cart items
            List<CartItem> cartItems = session.createQuery(
                            "from CartItem ci where ci.cart.id = :cid", CartItem.class)
                    .setParameter("cid", cart.getId())
                    .list();

            if (cartItems == null || cartItems.isEmpty()) throw new IllegalStateException("Giỏ hàng trống.");

            // 4) tạo Order
            Order order = new Order();
            order.setStudent(student);
            order.setCreatedAt(LocalDateTime.now());
            order.setStatus("PENDING_PAYMENT"); // để màn Payment load được

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CartItem ci : cartItems) {
                int quantity = (ci.getQuantity() == null ? 0 : ci.getQuantity());
                if (quantity <= 0) continue;

                Course course = session.get(Course.class, ci.getCourse().getId());
                if (course == null) continue;

                int available = (course.getAvailableSeats() == null ? 0 : course.getAvailableSeats());
                if (available < quantity) {
                    throw new IllegalStateException(
                            "Khóa học '" + course.getName() + "' không đủ chỗ! Còn " + available
                    );
                }

                // trừ chỗ (nếu bạn muốn chỉ trừ khi paid thì chuyển đoạn này sang lúc pay)
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

            if (order.getItems() == null || order.getItems().isEmpty()) {
                throw new IllegalStateException("Giỏ hàng không hợp lệ (quantity <= 0).");
            }

            // ==============================
            // ✅ 5) TẠO ENROLLMENT VÀ LINK VÀO ORDER
            // ==============================
            Enrollment en = new Enrollment();
            en.setStudent(student);

            // an toàn: nếu enum có PENDING_PAYMENT thì set được, không có thì nó tự fallback CART
            try {
                en.setStatus(EnrollmentStatus.PENDING_PAYMENT); // nếu enum bạn có
            } catch (Exception ignore) {
                en.setStatus("PENDING_PAYMENT"); // fallback theo String setter
            }

            en.setSubtotal(totalAmount);
            en.setDiscountAmount(BigDecimal.ZERO);
            en.setTaxAmount(BigDecimal.ZERO);
            en.setShippingFee(BigDecimal.ZERO);
            en.setPaidAmount(BigDecimal.ZERO);
            en.recalculateTotals(); // totalAmount = subtotal - discount + tax + ship

            session.persist(en);      // persist trước để có id
            order.setEnrollment(en);  // ✅ quan trọng: orders.enrollment_id != null

            // sync total về order để màn Payment tính due đúng
            order.setTotalAmount(en.getTotalAmount());

            // 6) lưu order
            session.persist(order);

            // 7) clear cart items
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
