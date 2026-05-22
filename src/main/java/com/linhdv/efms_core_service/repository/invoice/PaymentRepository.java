package com.linhdv.efms_core_service.repository.invoice;

import com.linhdv.efms_core_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("""
            SELECT p FROM Payment p
            WHERE p.companyId = :companyId
              AND (:type IS NULL OR p.paymentType = :type)
              AND (:partnerId IS NULL OR p.partner.id = :partnerId)
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    Page<Payment> search(
            @Param("companyId") UUID companyId,
            @Param("type") String type,
            @Param("partnerId") UUID partnerId,
            Pageable pageable
    );

    // ── Dashboard Queries ──────────────────────────────────────────────────────

    /** Tổng tiền thanh toán trong khoảng thời gian (dashboard KPI) */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.companyId = :companyId
              AND p.paymentDate >= :fromDate
              AND p.paymentDate <= :toDate
            """)
    BigDecimal sumPaymentInPeriod(
            @Param("companyId") UUID companyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /** Top N payment gần nhất kèm partner (dashboard Recent Payments) */
    @Query("""
            SELECT p FROM Payment p
            LEFT JOIN FETCH p.partner
            WHERE p.companyId = :companyId
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findRecentForDashboard(
            @Param("companyId") UUID companyId,
            Pageable pageable
    );
}
