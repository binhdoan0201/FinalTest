package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Course;
import com.ucop.edu.entity.Promotion;
import com.ucop.edu.entity.enums.PromotionDiscountType;
import com.ucop.edu.repository.PromotionRepository;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class PromotionService {

    private final PromotionRepository promotionRepo = new PromotionRepository();

    // ===== CRUD =====
    public Promotion saveOrUpdate(Promotion p, Long courseIdOrNull) {
        normalizePromotion(p, courseIdOrNull);
        validatePromotionDefinition(p);
        return promotionRepo.saveOrUpdate(p);
    }

    public Promotion findById(Long id) { return promotionRepo.findById(id); }
    public Promotion findByCode(String code) { return promotionRepo.findByCode(code); }
    public List<Promotion> findAll() { return promotionRepo.findAll(); }
    public void delete(Long id) { promotionRepo.delete(id); }

    /** Expire ngay lập tức */
    public void expireNow(Long id) {
        Promotion p = promotionRepo.findById(id);
        if (p == null) return;
        p.setValidTo(LocalDate.now().minusDays(1));
        promotionRepo.saveOrUpdate(p);
    }

    // ===== APPLY (preview/validate) - KHÔNG trừ lượt ở đây =====

    public static class ApplyResult {
        public final boolean ok;
        public final String message;
        public final BigDecimal discountAmount;
        public final Long promotionId;

        private ApplyResult(boolean ok, String message, BigDecimal discountAmount, Long promotionId) {
            this.ok = ok;
            this.message = message;
            this.discountAmount = discountAmount;
            this.promotionId = promotionId;
        }

        public static ApplyResult fail(String msg) {
            return new ApplyResult(false, msg, BigDecimal.ZERO, null);
        }

        public static ApplyResult ok(Long promotionId, BigDecimal discountAmount) {
            return new ApplyResult(true, "OK", discountAmount, promotionId);
        }
    }

    /**
     * Validate + tính số tiền giảm (preview).
     * ❗ Không tăng usedCount, không ghi PromotionUsage.
     */
    public ApplyResult applyPromotion(String code,
                                      BigDecimal totalAmount,
                                      Long courseIdRequiredOrNull) {
        if (code == null || code.trim().isEmpty()) return ApplyResult.fail("Mã khuyến mãi trống.");
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApplyResult.fail("Tổng tiền không hợp lệ.");
        }

        Promotion promo = promotionRepo.findByCode(code.trim());
        if (promo == null) return ApplyResult.fail("Mã khuyến mãi không tồn tại.");

        String ruleError = validatePromotionRuntime(promo, courseIdRequiredOrNull);
        if (ruleError != null) return ApplyResult.fail(ruleError);

        BigDecimal discount = computeDiscount(totalAmount, promo.getDiscountType(), promo.getDiscountValue());
        if (discount.compareTo(BigDecimal.ZERO) <= 0) return ApplyResult.fail("Giảm giá không hợp lệ.");

        return ApplyResult.ok(promo.getId(), discount);
    }

    // ===== helpers =====

    private void normalizePromotion(Promotion p, Long courseIdOrNull) {
        if (p.getCode() != null) p.setCode(p.getCode().trim().toUpperCase());

        Boolean applyToAll = p.isApplyToAll();
        if (applyToAll == null) applyToAll = true;
        p.setApplyToAll(applyToAll);

        if (applyToAll) {
            p.setCourse(null);
        } else if (courseIdOrNull != null) {
            try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                Course c = s.get(Course.class, courseIdOrNull);
                p.setCourse(c);
            }
        }

        if (p.getUsedCount() == null) p.setUsedCount(0);
    }

    private void validatePromotionDefinition(Promotion p) {
        if (p.getCode() == null || p.getCode().isBlank()) {
            throw new IllegalArgumentException("Code không được trống.");
        }
        if (p.getDiscountType() == null) {
            throw new IllegalArgumentException("Chưa chọn discountType.");
        }
        if (p.getDiscountValue() == null || p.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("discountValue phải > 0.");
        }

        if (p.getDiscountType() == PromotionDiscountType.PERCENT) {
            if (p.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percent không được > 100.");
            }
        }

        if (Boolean.FALSE.equals(p.isApplyToAll()) && p.getCourse() == null) {
            throw new IllegalArgumentException("Item-level phải chọn course.");
        }

        if (p.getValidFrom() != null && p.getValidTo() != null) {
            if (p.getValidFrom().isAfter(p.getValidTo())) {
                throw new IllegalArgumentException("validFrom không được sau validTo.");
            }
        }
    }

    private String validatePromotionRuntime(Promotion promo, Long courseIdRequiredOrNull) {
        LocalDate today = LocalDate.now();

        if (promo.getValidFrom() != null && today.isBefore(promo.getValidFrom())) return "Mã chưa đến ngày áp dụng.";
        if (promo.getValidTo() != null && today.isAfter(promo.getValidTo())) return "Mã đã hết hạn.";

        Integer max = promo.getMaxUsage();
        int used = promo.getUsedCount() == null ? 0 : promo.getUsedCount();
        if (max != null && used >= max) return "Mã đã hết lượt sử dụng.";

        boolean applyToAll = Boolean.TRUE.equals(promo.isApplyToAll());
        if (!applyToAll) {
            if (courseIdRequiredOrNull == null) return "Promo này chỉ áp dụng cho 1 khóa học cụ thể.";
            if (promo.getCourse() == null) return "Promo item-level thiếu course.";
            if (!Objects.equals(promo.getCourse().getId(), courseIdRequiredOrNull)) {
                return "Promo không áp dụng cho khóa học này.";
            }
        }
        return null;
    }

    private BigDecimal computeDiscount(BigDecimal totalAmount, PromotionDiscountType type, BigDecimal value) {
        if (type == null || value == null) return BigDecimal.ZERO;

        if ("PERCENT".equalsIgnoreCase(type.name())) {
            BigDecimal pct = value.divide(new BigDecimal("100"));
            BigDecimal d = totalAmount.multiply(pct);
            return d.min(totalAmount);
        }

        return value.min(totalAmount);
    }
}
