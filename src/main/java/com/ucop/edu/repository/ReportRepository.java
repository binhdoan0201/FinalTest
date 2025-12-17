package com.ucop.edu.repository;

import com.ucop.edu.dto.PaymentMethodRevenueDTO;
import com.ucop.edu.dto.RevenuePointDTO;
import com.ucop.edu.dto.TopCourseDTO;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {

    public List<RevenuePointDTO> revenueByDay(LocalDate from, LocalDate to) {
        Timestamp tsFrom = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tsTo = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql =
                "SELECT CAST(p.paid_at AS date) AS d, SUM(p.amount) AS total " +
                "FROM payments p " +
                "WHERE p.status = 'PAID' AND p.paid_at >= :from AND p.paid_at < :to " +
                "GROUP BY CAST(p.paid_at AS date) " +
                "ORDER BY d ASC";

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery(sql)
                    .setParameter("from", tsFrom)
                    .setParameter("to", tsTo)
                    .list();

            List<RevenuePointDTO> out = new ArrayList<>();
            for (Object[] r : rows) {
                Object d = r[0];
                BigDecimal total = toBigDecimal(r[1]);
                out.add(new RevenuePointDTO(String.valueOf(d), total));
            }
            return out;
        }
    }

    public List<RevenuePointDTO> revenueByMonth(LocalDate from, LocalDate to) {
        Timestamp tsFrom = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tsTo = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql =
                "SELECT CONCAT(YEAR(p.paid_at), '-', RIGHT('0' + CAST(MONTH(p.paid_at) AS varchar(2)), 2)) AS ym, " +
                "       SUM(p.amount) AS total " +
                "FROM payments p " +
                "WHERE p.status = 'PAID' AND p.paid_at >= :from AND p.paid_at < :to " +
                "GROUP BY YEAR(p.paid_at), MONTH(p.paid_at) " +
                "ORDER BY YEAR(p.paid_at), MONTH(p.paid_at)";

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery(sql)
                    .setParameter("from", tsFrom)
                    .setParameter("to", tsTo)
                    .list();

            List<RevenuePointDTO> out = new ArrayList<>();
            for (Object[] r : rows) {
                String ym = String.valueOf(r[0]);
                BigDecimal total = toBigDecimal(r[1]);
                out.add(new RevenuePointDTO(ym, total));
            }
            return out;
        }
    }

    public List<PaymentMethodRevenueDTO> revenueByPaymentMethod(LocalDate from, LocalDate to) {
        Timestamp tsFrom = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tsTo = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql =
                "SELECT p.payment_method, SUM(p.amount) AS total " +
                "FROM payments p " +
                "WHERE p.status = 'PAID' AND p.paid_at >= :from AND p.paid_at < :to " +
                "GROUP BY p.payment_method " +
                "ORDER BY total DESC";

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery(sql)
                    .setParameter("from", tsFrom)
                    .setParameter("to", tsTo)
                    .list();

            List<PaymentMethodRevenueDTO> out = new ArrayList<>();
            for (Object[] r : rows) {
                String method = (r[0] == null) ? null : String.valueOf(r[0]);
                BigDecimal total = toBigDecimal(r[1]);
                out.add(new PaymentMethodRevenueDTO(method, total));
            }
            return out;
        }
    }

    /**
     * ✅ FIX: Top khóa học theo đơn đã PAID (dựa theo payments.order_id).
     * Tránh JOIN bị nhân đôi nếu 1 order có nhiều payments => dùng EXISTS.
     *
     * Lưu ý: table/column theo convention dự án: order_items(course_id, order_id, quantity, unit_price, line_total)
     */
    public List<TopCourseDTO> topCourses(LocalDate from, LocalDate to, int limit) {
        Timestamp tsFrom = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tsTo = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql =
                "SELECT TOP (:limit) c.name AS course_name, " +
                "       SUM(ISNULL(oi.quantity, 0)) AS enroll_count, " +
                "       SUM(ISNULL(oi.line_total, ISNULL(oi.unit_price,0) * ISNULL(oi.quantity,0))) AS revenue " +
                "FROM order_items oi " +
                "JOIN courses c ON c.id = oi.course_id " +
                "WHERE EXISTS ( " +
                "   SELECT 1 FROM payments p " +
                "   WHERE p.order_id = oi.order_id " +
                "     AND p.status = 'PAID' " +
                "     AND p.paid_at >= :from AND p.paid_at < :to " +
                ") " +
                "GROUP BY c.name " +
                "ORDER BY revenue DESC";

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery(sql)
                    .setParameter("limit", limit)
                    .setParameter("from", tsFrom)
                    .setParameter("to", tsTo)
                    .list();

            List<TopCourseDTO> out = new ArrayList<>();
            for (Object[] r : rows) {
                String name = String.valueOf(r[0]);
                long count = toLong(r[1]);
                BigDecimal revenue = toBigDecimal(r[2]);
                out.add(new TopCourseDTO(name, count, revenue));
            }
            return out;
        }
    }

    public BigDecimal totalRefund(LocalDate from, LocalDate to) {
        Timestamp tsFrom = Timestamp.valueOf(from.atStartOfDay());
        Timestamp tsTo = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql =
                "SELECT SUM(r.amount) " +
                "FROM refunds r " +
                "WHERE r.processed_at >= :from AND r.processed_at < :to";

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Object v = s.createNativeQuery(sql)
                    .setParameter("from", tsFrom)
                    .setParameter("to", tsTo)
                    .uniqueResult();
            return toBigDecimal(v);
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        try { return new BigDecimal(String.valueOf(v)); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); }
        catch (Exception e) { return 0; }
    }
}
