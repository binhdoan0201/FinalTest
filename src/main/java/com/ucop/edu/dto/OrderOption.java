package com.ucop.edu.dto;

import java.math.BigDecimal;

public class OrderOption {
    private Long orderId;
    private Long enrollmentId;
    private String enrollmentCode;
    private BigDecimal due;
    private String orderStatus;

    public OrderOption(Long orderId, Long enrollmentId, String enrollmentCode, BigDecimal due, String orderStatus) {
        this.orderId = orderId;
        this.enrollmentId = enrollmentId;
        this.enrollmentCode = enrollmentCode;
        this.due = due;
        this.orderStatus = orderStatus;
    }

    public Long getOrderId() { return orderId; }
    public Long getEnrollmentId() { return enrollmentId; }
    public String getEnrollmentCode() { return enrollmentCode; }
    public BigDecimal getDue() { return due; }
    public String getOrderStatus() { return orderStatus; }
}
