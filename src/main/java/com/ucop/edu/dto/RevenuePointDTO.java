package com.ucop.edu.dto;

import java.math.BigDecimal;

public class RevenuePointDTO {
    private final String label;          // yyyy-MM-dd hoặc yyyy-MM
    private final BigDecimal totalRevenue;

    public RevenuePointDTO(String label, BigDecimal totalRevenue) {
        this.label = label;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
}
