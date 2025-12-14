package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import com.ucop.edu.entity.enums.PaymentMethod;
import com.ucop.edu.entity.enums.PaymentStatus;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private com.ucop.edu.entity.enums.PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private com.ucop.edu.entity.enums.PaymentStatus status = com.ucop.edu.entity.enums.PaymentStatus.PENDING;
    @Column(name = "transaction_id", length = 100)
    private String transactionId;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Refund> refunds = new HashSet<>();

    public Payment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public com.ucop.edu.entity.enums.PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(com.ucop.edu.entity.enums.PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public com.ucop.edu.entity.enums.PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(com.ucop.edu.entity.enums.PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Refund> getRefunds() {
        return refunds;
    }

    public void setRefunds(Set<Refund> refunds) {
        this.refunds = refunds;
    }

}
