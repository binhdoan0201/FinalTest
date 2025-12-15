package com.ucop.edu.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    // Giá hiện tại (có thể bằng giá mua, nhưng không bắt buộc)
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    // Giá tại thời điểm mua (nên dùng để tính tiền)
    @Column(name = "price_at_purchase", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtPurchase = BigDecimal.ZERO;

    // Tổng dòng = quantity * priceAtPurchase
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public OrderItem() {}

    // ================= LIFECYCLE =================
    @PrePersist
    @PreUpdate
    private void syncAndCalc() {
        int q = (quantity == null ? 0 : quantity);

        // Ưu tiên priceAtPurchase để tính
        BigDecimal purchasePrice = nz(priceAtPurchase);

        // Nếu chưa set priceAtPurchase (hoặc =0) thì fallback qua unitPrice
        if (purchasePrice.compareTo(BigDecimal.ZERO) == 0) {
            purchasePrice = nz(unitPrice);
            this.priceAtPurchase = purchasePrice; // đồng bộ để DB không lệch
        }

        // Đồng bộ unitPrice để nhìn UI thống nhất (tuỳ bạn muốn, mình set cho an toàn)
        this.unitPrice = purchasePrice;

        // Tính lineTotal
        this.lineTotal = purchasePrice.multiply(BigDecimal.valueOf(q));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ================= GETTER / SETTER =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = nz(unitPrice);
        // Không ép priceAtPurchase ở đây nữa, để syncAndCalc xử lý 1 chỗ
    }

    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) {
        this.priceAtPurchase = nz(priceAtPurchase);
    }

    public BigDecimal getLineTotal() { return lineTotal; }
    // ✅ Không cho set lineTotal từ ngoài, để luôn đúng theo công thức
    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = nz(lineTotal);
    }

    // ================= equals/hashCode =================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem other = (OrderItem) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
