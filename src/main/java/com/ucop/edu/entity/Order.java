package com.ucop.edu.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== STUDENT (Account) =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    // ===== CREATED TIME =====
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ===== TOTAL =====
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ===== STATUS =====
    // DB bạn chưa có CHECK cho orders.status nên để String ok
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

    // ================= LIFECYCLE =================
    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (status == null || status.isBlank()) status = "PENDING";
        // đảm bảo 2 chiều nếu có items trước khi persist
        if (items != null) {
            for (OrderItem it : items) {
                if (it != null) it.setOrder(this);
            }
        }
        recalculateTotal();
    }

    @PreUpdate
    protected void preUpdate() {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (status == null || status.isBlank()) status = "PENDING";
        recalculateTotal();
    }

    // ================= HELPER METHODS =================
    /** ✅ Bắt buộc set quan hệ 2 chiều */
    public void addItem(OrderItem item) {
        if (item == null) return;
        if (items == null) items = new HashSet<>();
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        if (item == null || items == null) return;
        items.remove(item);
        item.setOrder(null);
        recalculateTotal();
    }

    public void clearItems() {
        if (items == null) return;
        for (OrderItem it : items) {
            if (it != null) it.setOrder(null);
        }
        items.clear();
        recalculateTotal();
    }

    /** ✅ Tính lại totalAmount từ items (an toàn null) */
    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItem it : items) {
                if (it == null) continue;

                BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                Integer qty = it.getQuantity() == null ? 0 : it.getQuantity();

                sum = sum.add(price.multiply(BigDecimal.valueOf(qty)));
            }
        }
        totalAmount = sum;
    }

    // ================= GETTER / SETTER =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getStudent() { return student; }
    public void setStudent(Account student) { this.student = student; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = (totalAmount == null) ? BigDecimal.ZERO : totalAmount;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = (status == null || status.isBlank()) ? "PENDING" : status;
    }

    public Set<OrderItem> getItems() { return items; }
    public void setItems(Set<OrderItem> items) {
        this.items = (items == null) ? new HashSet<>() : items;
        for (OrderItem it : this.items) {
            if (it != null) it.setOrder(this);
        }
        recalculateTotal();
    }

    // ================= equals/hashCode =================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order other = (Order) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
