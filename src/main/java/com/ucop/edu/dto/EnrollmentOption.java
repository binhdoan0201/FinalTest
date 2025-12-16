package com.ucop.edu.dto;

import java.math.BigDecimal;

public class EnrollmentOption {
    private final Long id;
    private final String code;
    private final BigDecimal due;

    public EnrollmentOption(Long id, String code, BigDecimal due) {
        this.id = id;
        this.code = code;
        this.due = due;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public BigDecimal getDue() { return due; }
}
