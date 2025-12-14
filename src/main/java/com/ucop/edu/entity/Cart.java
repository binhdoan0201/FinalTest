package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Account student;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "cart", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> items = new HashSet<>();

    public Cart() {}

    // ================= BUSINESS METHODS =================

    public void addCourse(Course course) {
        if (course == null || course.getId() == null) return;

        CartItem existing = null;
        for (CartItem i : items) {
            if (i.getCourse() != null && course.getId().equals(i.getCourse().getId())) {
                existing = i;
                break;
            }
        }

        if (existing != null) {
            int q = existing.getQuantity() == null ? 0 : existing.getQuantity();
            existing.setQuantity(q + 1);
        } else {
            CartItem item = new CartItem();
            item.setCart(this);
            item.setCourse(course);
            item.setQuantity(1);

            // ✅ FIX: Course của bạn dùng tuitionFee (BigDecimal)
            item.setPriceAtAdd(course.getTuitionFee());

            items.add(item);
        }
    }

    public void updateQuantity(Long courseId, int quantity) {
        if (courseId == null) return;

        CartItem target = null;
        for (CartItem i : items) {
            if (i.getCourse() != null && courseId.equals(i.getCourse().getId())) {
                target = i;
                break;
            }
        }

        if (target == null) return;

        if (quantity <= 0) {
            removeCourse(courseId);
        } else {
            target.setQuantity(quantity);
        }
    }

    public void removeCourse(Long courseId) {
        if (courseId == null) return;
        items.removeIf(i -> i.getCourse() != null && courseId.equals(i.getCourse().getId()));
    }

    public void clear() {
        items.clear();
    }

    @Transient
    public BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem i : items) {
            total = total.add(i.getLineTotal());
        }
        return total;
    }

    // ================= GETTERS / SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getStudent() { return student; }
    public void setStudent(Account student) { this.student = student; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<CartItem> getItems() { return items; }
    public void setItems(Set<CartItem> items) { this.items = items; }
}
