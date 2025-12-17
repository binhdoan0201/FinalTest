package com.ucop.edu.entity;

import com.ucop.edu.entity.enums.PromotionDiscountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Promotion/Voucher.
 * usedCount sẽ CHỈ tăng khi đơn thanh toán THÀNH CÔNG (Order -> PAID).
 * Apply mã ở màn Student chỉ "preview/attach" vào Enrollment, KHÔNG trừ lượt.
 */
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "name", length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 30, nullable = false)
    private PromotionDiscountType discountType;

    @Column(name = "discount_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    /** true = áp cho CART/Enrollment (tổng), false = áp cho ITEM (1 course) */
    @Column(name = "apply_to_all")
    private Boolean applyToAll = Boolean.TRUE;

    /** số lượt tối đa, null = không giới hạn */
    @Column(name = "max_usage")
    private Integer maxUsage;

    /** số lượt đã dùng (chỉ tăng khi pay xong) */
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // ================= getters/setters =================

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PromotionDiscountType getDiscountType() { return discountType; }
    public void setDiscountType(PromotionDiscountType discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public Boolean isApplyToAll() { return applyToAll; }
    public void setApplyToAll(Boolean applyToAll) { this.applyToAll = applyToAll; }

    public Integer getMaxUsage() { return maxUsage; }
    public void setMaxUsage(Integer maxUsage) { this.maxUsage = maxUsage; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    // ================= helper for ADMIN UI =================

    @Transient
    public Integer getRemainingUsage() {
        if (maxUsage == null) return null; // unlimited
        int used = usedCount == null ? 0 : usedCount;
        int remain = maxUsage - used;
        return Math.max(0, remain);
    }

    /** Text hiển thị trong TableView admin. Ví dụ: "4" hoặc "∞". */
    @Transient
    public String getRemainingUsageText() {
        Integer remain = getRemainingUsage();
        if (remain == null) return "∞";
        return String.valueOf(remain);
    }
}
