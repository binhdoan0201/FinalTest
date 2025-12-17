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

    // ✅ LINK sang Enrollment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    // ===== CREATED TIME =====
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ===== TOTAL (có thể đã áp voucher) =====
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

    // =========================================================
    // ✅ UI / Controller needs (không lưu DB)
    // để StudentPaymentController compile được (getPromotion, getSubtotalAmount...)
    // =========================================================
    @Transient
    private Promotion promotion;

    @Transient
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Transient
    private BigDecimal discountAmount = BigDecimal.ZERO;

    public Order() {}

    // ================= LIFECYCLE =================
    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (status == null || status.isBlank()) status = "PENDING";

        // đảm bảo item trỏ ngược về order
        if (items != null) {
            for (OrderItem it : items) {
                if (it != null) it.setOrder(this);
            }
        }

        // recalc nhưng KHÔNG phá totalAmount nếu totalAmount đã được set khác sum-items (voucher)
        recalculateTotal();
    }

    @PreUpdate
    protected void preUpdate() {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (status == null || status.isBlank()) status = "PENDING";

        // đảm bảo item trỏ ngược về order
        if (items != null) {
            for (OrderItem it : items) {
                if (it != null) it.setOrder(this);
            }
        }

        recalculateTotal();
    }

    // ================= HELPER METHODS =================
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

    /**
     * ✅ Tính tổng theo item.
     * - Nếu totalAmount đang null/0 hoặc đang đúng bằng sum-items => set totalAmount = sum-items
     * - Nếu totalAmount đã được set khác sum-items (ví dụ đã trừ voucher) => GIỮ NGUYÊN totalAmount
     */
    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;

        if (items != null) {
            for (OrderItem it : items) {
                if (it == null) continue;

                BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                Integer qty = it.getQuantity() == null ? 0 : it.getQuantity();

                if (qty > 0) {
                    sum = sum.add(price.multiply(BigDecimal.valueOf(qty)));
                }
            }
        }

        if (sum.signum() < 0) sum = BigDecimal.ZERO;

        // nếu totalAmount chưa set/đang 0 => set theo sum
        if (totalAmount == null || totalAmount.signum() == 0) {
            totalAmount = sum;
            return;
        }

        // nếu totalAmount đang đúng bằng sum => vẫn set (cho chắc)
        if (totalAmount.compareTo(sum) == 0) {
            totalAmount = sum;
            return;
        }

        // ✅ còn lại: totalAmount đã khác sum (thường là do voucher/discount) => giữ nguyên
    }

    // ================= GETTER / SETTER =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getStudent() { return student; }
    public void setStudent(Account student) { this.student = student; }

    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }

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

    // ======= UI-only fields getter/setter =======
    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = (subtotalAmount == null) ? BigDecimal.ZERO : subtotalAmount;
    }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = (discountAmount == null) ? BigDecimal.ZERO : discountAmount;
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
