package com.linhdv.efms_core_service.repository.invoice;

import com.linhdv.efms_core_service.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.companyId = :companyId
              AND (:type IS NULL OR i.invoiceType = :type)
              AND (:status IS NULL OR i.status = :status)
              AND (:partnerId IS NULL OR i.partner.id = :partnerId)
            ORDER BY i.invoiceDate DESC, i.createdAt DESC
            """)
    Page<Invoice> search(
            @Param("companyId") UUID companyId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("partnerId") UUID partnerId,
            Pageable pageable
    );

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.companyId = :companyId
              AND i.status IN ('open', 'in_payment')
              AND i.dueDate < :currentDate
            ORDER BY i.dueDate ASC
            """)
    List<Invoice> findOverdue(
            @Param("companyId") UUID companyId,
            @Param("currentDate") LocalDate currentDate
    );

    // Lịch sử hóa đơn của 1 đối tác
    List<Invoice> findByPartnerIdOrderByInvoiceDateDesc(UUID partnerId);

    // ── Dashboard Queries ──────────────────────────────────────────────────────

    /** Tổng totalAmount của AR invoice đang mở (phải thu) */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice i
            WHERE i.companyId = :companyId
              AND i.invoiceType = 'AR'
              AND i.status IN ('open', 'in_payment')
            """)
    BigDecimal sumOpenAr(@Param("companyId") UUID companyId);

    /** Tổng totalAmount của AP invoice đang mở (phải trả) */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice i
            WHERE i.companyId = :companyId
              AND i.invoiceType = 'AP'
              AND i.status IN ('open', 'in_payment')
            """)
    BigDecimal sumOpenAp(@Param("companyId") UUID companyId);

    /** Đếm hóa đơn theo từng nhóm status cho Pie chart: [status, count] */
    @Query("""
            SELECT i.status, i.approvalStatus, COUNT(i)
            FROM Invoice i
            WHERE i.companyId = :companyId
            GROUP BY i.status, i.approvalStatus
            """)
    List<Object[]> countByStatusAndApprovalStatus(@Param("companyId") UUID companyId);

    /** Tổng hợp doanh thu/chi phí theo tháng: [year, month, invoiceType, sum] */
    @Query("""
            SELECT YEAR(i.invoiceDate), MONTH(i.invoiceDate), i.invoiceType, SUM(i.totalAmount)
            FROM Invoice i
            WHERE i.companyId = :companyId
              AND i.invoiceDate >= :fromDate
              AND i.status NOT IN ('draft', 'cancelled')
            GROUP BY YEAR(i.invoiceDate), MONTH(i.invoiceDate), i.invoiceType
            ORDER BY YEAR(i.invoiceDate), MONTH(i.invoiceDate)
            """)
    List<Object[]> sumByMonthAndType(
            @Param("companyId") UUID companyId,
            @Param("fromDate") LocalDate fromDate
    );

    /** Top N hóa đơn gần nhất cần xử lý (draft hoặc open/pending) */
    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.partner
            WHERE i.companyId = :companyId
              AND (
                    i.status = 'draft'
                    OR (i.status = 'open' AND (i.approvalStatus = 'pending' OR i.invoiceType = 'AR'))
              )
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findPendingForDashboard(
            @Param("companyId") UUID companyId,
            Pageable pageable
    );
}
