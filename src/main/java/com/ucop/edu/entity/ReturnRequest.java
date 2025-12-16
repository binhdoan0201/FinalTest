package com.ucop.edu.entity;

import com.ucop.edu.entity.enums.ReturnRequestStatus;
import com.ucop.edu.entity.enums.ReturnRequestType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB: enrollment_id NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    // DB: student_id NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    // DB: order_id NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // DB: payment_id NULL (nếu bạn muốn link)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private ReturnRequestType type = ReturnRequestType.REFUND;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReturnRequestStatus status = ReturnRequestStatus.REQUESTED;

    @Column(name = "refund_amount", precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "reason", length = 500)
    private String reason;

    // DB: requested_at NOT NULL
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime createdAt;

    // DB: processed_at NULL
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "staff_note", length = 1000)
    private String staffNote;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (type == null) type = ReturnRequestType.REFUND;
        if (status == null) status = ReturnRequestStatus.REQUESTED;
    }

    // ===== getters/setters =====
    public Long getId() { return id; }

    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }

    public Account getStudent() { return student; }
    public void setStudent(Account student) { this.student = student; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public ReturnRequestType getType() { return type; }
    public void setType(ReturnRequestType type) { this.type = type; }

    public ReturnRequestStatus getStatus() { return status; }
    public void setStatus(ReturnRequestStatus status) { this.status = status; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }
}
