package com.ucop.edu.dto;

import java.math.BigDecimal;

public class PaymentMethodRevenueDTO {
    private final String method;
    private final BigDecimal total;

    public PaymentMethodRevenueDTO(String method, BigDecimal total) {
        this.method = (method == null || method.isBlank()) ? "UNKNOWN" : method;
        this.total = total == null ? BigDecimal.ZERO : total;
    }

    public String getMethod() {
        return method;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
