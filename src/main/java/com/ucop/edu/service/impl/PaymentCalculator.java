package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Enrollment;
import com.ucop.edu.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentCalculator {

    // Config (sau này đưa vào bảng config)
    public static final BigDecimal VAT_RATE = new BigDecimal("0.10");         // 10%
    public static final BigDecimal COD_FEE = new BigDecimal("15000");         // phí COD cố định
    public static final BigDecimal GATEWAY_FEE_RATE = new BigDecimal("0.02"); // 2%

    public static Result calc(Enrollment e, PaymentMethod method) {
        BigDecimal subtotal = nz(e.getSubtotal());
        BigDecimal discount = nz(e.getDiscountAmount());

        BigDecimal taxable = subtotal.subtract(discount);
        if (taxable.signum() < 0) taxable = BigDecimal.ZERO;

        BigDecimal tax = taxable.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal shippingFee = nz(e.getShippingFee()); // course thường = 0
        BigDecimal codFee = BigDecimal.ZERO;
        BigDecimal gatewayFee = BigDecimal.ZERO;

        if (method == PaymentMethod.COD) {
            codFee = COD_FEE;
        } else if (method == PaymentMethod.GATEWAY) {
            gatewayFee = taxable.multiply(GATEWAY_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = taxable
                .add(tax)
                .add(shippingFee)
                .add(codFee)
                .add(gatewayFee)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal paid = nz(e.getPaidAmount());
        BigDecimal amountDue = grandTotal.subtract(paid);
        if (amountDue.signum() < 0) amountDue = BigDecimal.ZERO;

        return new Result(subtotal, discount, tax, shippingFee, codFee, gatewayFee, grandTotal, amountDue);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public record Result(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal tax,
            BigDecimal shippingFee,
            BigDecimal codFee,
            BigDecimal gatewayFee,
            BigDecimal grandTotal,
            BigDecimal amountDue
    ) {}
}
