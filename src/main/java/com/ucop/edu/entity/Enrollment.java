package com.ucop.edu.entity;

import com.ucop.edu.entity.enums.EnrollmentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_code", unique = true, length = 30)
    private String enrollmentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id") // DB đang cho phép NULL
    private Account student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private EnrollmentStatus status = EnrollmentStatus.CART;

    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "enrollment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EnrollmentItem> items = new HashSet<>();

    @OneToMany(mappedBy = "enrollment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Appointment> appointments = new HashSet<>();

    @OneToMany(mappedBy = "enrollment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();

    public Enrollment() {}

    // ================= LIFECYCLE =================
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;

        normalizeMoney();
        ensureBidirectionalLinks();
        recalculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        normalizeMoney();
        ensureBidirectionalLinks();
        recalculateTotals();
    }

    private void normalizeMoney() {
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (status == null) status = EnrollmentStatus.CART;
    }

    private void ensureBidirectionalLinks() {
        if (items != null) for (EnrollmentItem it : items) if (it != null) it.setEnrollment(this);
        if (appointments != null) for (Appointment ap : appointments) if (ap != null) ap.setEnrollment(this);
        if (payments != null) for (Payment p : payments) if (p != null) p.setEnrollment(this);
    }

    // ================= BUSINESS =================
    /** Tính subtotal từ items; total = subtotal - discount + tax + shipping */
    public void recalculateTotals() {
        BigDecimal sub = BigDecimal.ZERO;

        if (items != null) {
            for (EnrollmentItem it : items) {
                if (it == null) continue;

                BigDecimal totalPrice = it.getTotalPrice();
                if (totalPrice != null) {
                    sub = sub.add(totalPrice);
                } else {
                    // fallback nếu EnrollmentItem chưa set totalPrice
                    BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                    Integer qty = it.getQuantity() == null ? 0 : it.getQuantity();
                    BigDecimal itemDiscount = it.getDiscountItemDiscount() == null ? BigDecimal.ZERO : it.getDiscountItemDiscount();
                    BigDecimal line = unit.multiply(BigDecimal.valueOf(qty)).subtract(itemDiscount);
                    if (line.signum() < 0) line = BigDecimal.ZERO;
                    sub = sub.add(line);
                }
            }
        }

        subtotal = sub;

        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal tax = taxAmount == null ? BigDecimal.ZERO : taxAmount;
        BigDecimal ship = shippingFee == null ? BigDecimal.ZERO : shippingFee;

        BigDecimal total = subtotal.subtract(discount);
        if (total.signum() < 0) total = BigDecimal.ZERO;
        total = total.add(tax).add(ship);

        totalAmount = total;
    }

    /** Số tiền còn phải trả */
    public BigDecimal getAmountDue() {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal due = total.subtract(paid);
        return due.signum() < 0 ? BigDecimal.ZERO : due;
    }

    // ================= HELPER 2 CHIỀU =================
    public void addItem(EnrollmentItem item) {
        if (item == null) return;
        items.add(item);
        item.setEnrollment(this);
        recalculateTotals();
    }

    public void removeItem(EnrollmentItem item) {
        if (item == null) return;
        items.remove(item);
        item.setEnrollment(null);
        recalculateTotals();
    }

    public void addAppointment(Appointment ap) {
        if (ap == null) return;
        appointments.add(ap);
        ap.setEnrollment(this);
    }

    public void removeAppointment(Appointment ap) {
        if (ap == null) return;
        appointments.remove(ap);
        ap.setEnrollment(null);
    }

    public void addPayment(Payment p) {
        if (p == null) return;
        payments.add(p);
        p.setEnrollment(this);
    }

    public void removePayment(Payment p) {
        if (p == null) return;
        payments.remove(p);
        p.setEnrollment(null);
    }

    // ================= GETTER / SETTER =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEnrollmentCode() { return enrollmentCode; }
    public void setEnrollmentCode(String enrollmentCode) { this.enrollmentCode = enrollmentCode; }

    public Account getStudent() { return student; }
    public void setStudent(Account student) { this.student = student; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) {
        this.status = (status == null) ? EnrollmentStatus.CART : status;
    }

    /** ✅ Cho bạn gọi setStatus("PAID") từ code UI */
    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            this.status = EnrollmentStatus.CART;
            return;
        }
        try {
            this.status = EnrollmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            this.status = EnrollmentStatus.CART;
        }
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal == null ? BigDecimal.ZERO : subtotal; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount == null ? BigDecimal.ZERO : taxAmount; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<EnrollmentItem> getItems() { return items; }
    public void setItems(Set<EnrollmentItem> items) {
        this.items = (items == null) ? new HashSet<>() : items;
        ensureBidirectionalLinks();
        recalculateTotals();
    }

    public Set<Appointment> getAppointments() { return appointments; }
    public void setAppointments(Set<Appointment> appointments) {
        this.appointments = (appointments == null) ? new HashSet<>() : appointments;
        ensureBidirectionalLinks();
    }

    public Set<Payment> getPayments() { return payments; }
    public void setPayments(Set<Payment> payments) {
        this.payments = (payments == null) ? new HashSet<>() : payments;
        ensureBidirectionalLinks();
    }

    // ================= equals/hashCode =================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enrollment)) return false;
        Enrollment that = (Enrollment) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
