package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== STUDENT =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    // ===== CREATED TIME =====
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ===== TOTAL =====
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ===== STATUS =====
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    // ===== ITEMS =====
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<OrderItem> items = new HashSet<>();

    public Order() {}

    // ================= HELPER METHODS =================
    // ✅ BẮT BUỘC: set quan hệ 2 chiều để Hibernate insert đúng
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    // ================= GETTER / SETTER =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getStudent() { return student; }
    public void setStudent(Account student) { this.student = student; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Set<OrderItem> getItems() { return items; }
    public void setItems(Set<OrderItem> items) { this.items = items; }
}
