package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import com.ucop.edu.entity.enums.PromotionDiscountType;

@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;
    @Column(name = "name", length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 10)
    private com.ucop.edu.entity.enums.PromotionDiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "max_usage")
    private Integer maxUsage;
    @Column(name = "used_count")
    private Integer usedCount = 0;
    @Column(name = "valid_from")
    private LocalDate validFrom;
    @Column(name = "valid_to")
    private LocalDate validTo;
    @Column(name = "apply_to_all")
    private Boolean applyToAll = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private Set<PromotionUsage> usages = new HashSet<>();
    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private Set<Enrollment> enrollments = new HashSet<>();

    public Promotion() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public com.ucop.edu.entity.enums.PromotionDiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(com.ucop.edu.entity.enums.PromotionDiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public Integer getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(Integer maxUsage) {
        this.maxUsage = maxUsage;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Boolean isApplyToAll() {
        return applyToAll;
    }

    public void setApplyToAll(Boolean applyToAll) {
        this.applyToAll = applyToAll;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<PromotionUsage> getUsages() {
        return usages;
    }

    public void setUsages(Set<PromotionUsage> usages) {
        this.usages = usages;
    }

    public Set<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(Set<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

}
