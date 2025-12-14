package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "course_id"})
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "price_at_add", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAdd;

    public CartItem() {}

    @Transient
    public BigDecimal getLineTotal() {
        BigDecimal price = priceAtAdd == null ? BigDecimal.ZERO : priceAtAdd;
        int qty = quantity == null ? 0 : quantity;
        return price.multiply(BigDecimal.valueOf(qty));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPriceAtAdd() { return priceAtAdd; }
    public void setPriceAtAdd(BigDecimal priceAtAdd) { this.priceAtAdd = priceAtAdd; }
}
