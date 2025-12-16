package com.ucop.edu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReturnRequestRow {
    private Long id;
    private String orderCode;      // ví dụ "#25"
    private String type;
    private String status;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;

    public ReturnRequestRow(Long id, String orderCode, String type, String status, BigDecimal refundAmount, LocalDateTime createdAt) {
        this.id = id;
        this.orderCode = orderCode;
        this.type = type;
        this.status = status;
        this.refundAmount = refundAmount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
