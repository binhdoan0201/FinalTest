package com.ucop.edu.service.impl;

import com.ucop.edu.dto.PaymentMethodRevenueDTO;
import com.ucop.edu.dto.RevenuePointDTO;
import com.ucop.edu.dto.TopCourseDTO;
import com.ucop.edu.repository.ReportRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {

    private final ReportRepository repo = new ReportRepository();

    public List<RevenuePointDTO> revenueByDay(LocalDate from, LocalDate to) {
        return repo.revenueByDay(from, to);
    }

    public List<RevenuePointDTO> revenueByMonth(LocalDate from, LocalDate to) {
        return repo.revenueByMonth(from, to);
    }

    public List<PaymentMethodRevenueDTO> revenueByPaymentMethod(LocalDate from, LocalDate to) {
        return repo.revenueByPaymentMethod(from, to);
    }

    public List<TopCourseDTO> topCourses(LocalDate from, LocalDate to, int limit) {
        return repo.topCourses(from, to, limit);
    }

    public BigDecimal totalRefund(LocalDate from, LocalDate to) {
        return repo.totalRefund(from, to);
    }
}
