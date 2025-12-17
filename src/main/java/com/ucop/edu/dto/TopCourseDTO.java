package com.ucop.edu.dto;

import java.math.BigDecimal;

public class TopCourseDTO {
    private final String courseName;
    private final long enrollCount;
    private final BigDecimal revenue;

    public TopCourseDTO(String courseName, long enrollCount, BigDecimal revenue) {
        this.courseName = courseName;
        this.enrollCount = enrollCount;
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }

    public String getCourseName() {
        return courseName;
    }

    public long getEnrollCount() {
        return enrollCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
