package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    // ✅ DB đang có unit_price NOT NULL
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    // ✅ DB đang có price_at_purchase NOT NULL
    @Column(name = "price_at_purchase", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtPurchase = BigDecimal.ZERO;

    // ✅ DB đang có line_total NOT NULL
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public OrderItem() {}

    @PrePersist
    @PreUpdate
    private void calcLineTotal() {
        int q = (quantity == null ? 0 : quantity);
        BigDecimal price = (unitPrice == null ? BigDecimal.ZERO : unitPrice);
        this.lineTotal = price.multiply(BigDecimal.valueOf(q));

        // đồng bộ luôn để cột price_at_purchase không bị NULL
        if (this.priceAtPurchase == null) {
            this.priceAtPurchase = price;
        }
    }

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
        this.unitPrice = (unitPrice == null ? BigDecimal.ZERO : unitPrice);
    }

    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) {
        this.priceAtPurchase = (priceAtPurchase == null ? BigDecimal.ZERO : priceAtPurchase);
        // đồng bộ sang unitPrice để tính line_total
        this.unitPrice = this.priceAtPurchase;
    }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = (lineTotal == null ? BigDecimal.ZERO : lineTotal);
    }
}
