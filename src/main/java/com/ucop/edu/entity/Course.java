package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "tuition_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal tuitionFee;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private com.ucop.edu.entity.enums.CourseStatus status =
            com.ucop.edu.entity.enums.CourseStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // ===== HÌNH ẢNH KHÓA HỌC =====
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ===== CÁC QUAN HỆ (GIỮ NGUYÊN) =====
    @OneToOne(mappedBy = "course", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private SeatStock seatStock;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<CartItem> cartItems = new HashSet<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<EnrollmentItem> enrollmentItems = new HashSet<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<Appointment> appointments = new HashSet<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<Promotion> promotions = new HashSet<>();

    // ===== DEMO SV3: SỐ NGƯỜI ĐÃ ĐĂNG KÝ =====
    // KHÔNG lưu DB – chỉ dùng hiển thị
    @Transient
    public Integer getRegisteredCount() {
        if (maxSeats == null || availableSeats == null) return 0;
        return maxSeats - availableSeats;
    }

    public Course() {}

    // ===== GETTER / SETTER =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public Integer getMaxSeats() { return maxSeats; }
    public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public com.ucop.edu.entity.enums.CourseStatus getStatus() { return status; }
    public void setStatus(com.ucop.edu.entity.enums.CourseStatus status) { this.status = status; }

    public Categories getCategory() { return category; }
    public void setCategory(Categories category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public SeatStock getSeatStock() { return seatStock; }
    public void setSeatStock(SeatStock seatStock) { this.seatStock = seatStock; }

    public Set<CartItem> getCartItems() { return cartItems; }
    public void setCartItems(Set<CartItem> cartItems) { this.cartItems = cartItems; }

    public Set<EnrollmentItem> getEnrollmentItems() { return enrollmentItems; }
    public void setEnrollmentItems(Set<EnrollmentItem> enrollmentItems) {
        this.enrollmentItems = enrollmentItems;
    }

    public Set<Appointment> getAppointments() { return appointments; }
    public void setAppointments(Set<Appointment> appointments) {
        this.appointments = appointments;
    }

    public Set<Promotion> getPromotions() { return promotions; }
    public void setPromotions(Set<Promotion> promotions) {
        this.promotions = promotions;
    }
}
