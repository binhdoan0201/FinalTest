package com.ucop.edu.repository;

import com.ucop.edu.dto.OrderOption;
import com.ucop.edu.dto.ReturnRequestRow;
import com.ucop.edu.entity.ReturnRequest;
import com.ucop.edu.entity.enums.ReturnRequestStatus;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class ReturnRequestRepository {

    // Dropdown chọn đơn giống thanh toán
    public List<OrderOption> findOrderOptionsForStudent(Long studentId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select new com.ucop.edu.dto.OrderOption(" +
                            " o.id, e.id, e.enrollmentCode, " +
                            " (coalesce(e.totalAmount,0) - coalesce(e.paidAmount,0)), " +
                            " concat(o.status,'') " +
                            ") " +
                            "from Order o " +
                            "join o.student st " +
                            "join o.enrollment e " +
                            "where st.id = :sid " +
                            "order by o.id desc",
                    OrderOption.class
            ).setParameter("sid", studentId).list();
        }
    }

    // History hiển thị mã đơn "#id"
    public List<ReturnRequestRow> findHistoryRowsByStudent(Long studentId) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "select new com.ucop.edu.dto.ReturnRequestRow(" +
                            " rr.id, " +
                            " case when o.id is null then '' else concat('#', o.id) end, " +
                            " concat(rr.type,''), " +
                            " concat(rr.status,''), " +
                            " rr.refundAmount, " +
                            " rr.createdAt " +
                            ") " +
                            "from ReturnRequest rr " +
                            "join rr.student st " +
                            "left join rr.order o " +
                            "where st.id = :sid " +
                            "order by rr.id desc",
                    ReturnRequestRow.class
            ).setParameter("sid", studentId).list();
        }
    }

    public Long countActiveByEnrollment(Session s, Long enrollmentId) {
        return s.createQuery(
                "select count(rr.id) from ReturnRequest rr " +
                        "where rr.enrollment.id = :eid and rr.status in (:st1,:st2)",
                Long.class
        ).setParameter("eid", enrollmentId)
         .setParameter("st1", ReturnRequestStatus.REQUESTED)
         .setParameter("st2", ReturnRequestStatus.APPROVED)
         .uniqueResult();
    }

    // ===== STAFF =====
    public List<ReturnRequest> findRequestedFetchUI(Session s) {
        return s.createQuery(
                "select rr from ReturnRequest rr " +
                        "join fetch rr.enrollment e " +
                        "join fetch rr.student st " +
                        "left join fetch rr.order o " +
                        "where rr.status = :st " +
                        "order by rr.createdAt desc",
                ReturnRequest.class
        ).setParameter("st", ReturnRequestStatus.REQUESTED)
         .list();
    }

    public ReturnRequest findByIdFetchUI(Session s, Long id) {
        return s.createQuery(
                "select rr from ReturnRequest rr " +
                        "join fetch rr.enrollment e " +
                        "join fetch rr.student st " +
                        "left join fetch rr.order o " +
                        "where rr.id = :id",
                ReturnRequest.class
        ).setParameter("id", id)
         .uniqueResult();
    }
}
