package com.linhdv.efms_core_service.repository.invoice;

import com.linhdv.efms_core_service.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    java.util.Optional<Invoice> findByCamundaProcessId(String camundaProcessId);
}
