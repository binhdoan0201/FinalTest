package com.ucop.edu.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Log mỗi lần mã được TIÊU THỤ (khi Order thanh toán đủ).
 */
@Entity
@Table(name = "promotion_usages")
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "used_by", nullable = false)
    private Account usedBy;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public Long getId() { return id; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public Account getUsedBy() { return usedBy; }
    public void setUsedBy(Account usedBy) { this.usedBy = usedBy; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
