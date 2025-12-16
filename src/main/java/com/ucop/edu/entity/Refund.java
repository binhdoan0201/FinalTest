package com.ucop.edu.entity;

import com.ucop.edu.entity.enums.RefundStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB: refunds.payment_id -> payments.id (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    // Nếu DB là nvarchar(max) thì ok; nếu DB là nvarchar(500) thì đổi length=500 và bỏ Lob
    @Column(name = "reason", columnDefinition = "nvarchar(max)")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Refund() {}

    @PrePersist
    public void prePersist() {
        if (status == null) status = RefundStatus.REQUESTED;
        if (amount == null) amount = BigDecimal.ZERO;

        // nếu tạo mới mà đã PROCESSED thì set luôn
        if (processedAt == null && status == RefundStatus.PROCESSED) {
            processedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (status == null) status = RefundStatus.REQUESTED;
        if (amount == null) amount = BigDecimal.ZERO;

        // ✅ Fix lỗi phổ biến: update status sang PROCESSED nhưng processedAt vẫn null
        if (processedAt == null && status == RefundStatus.PROCESSED) {
            processedAt = LocalDateTime.now();
        }
    }

    // ===== getters/setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = (amount == null ? BigDecimal.ZERO : amount);
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) {
        this.status = (status == null ? RefundStatus.REQUESTED : status);
    }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
