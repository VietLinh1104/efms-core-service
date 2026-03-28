package com.linhdv.efms_core_service.repository.accounting;

import com.linhdv.efms_core_service.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    /** Tìm kiếm chứng từ theo công ty, trạng thái, khoảng ngày */
    @Query("""
            SELECT j FROM JournalEntry j
            WHERE j.companyId = :companyId
              AND (:status IS NULL OR j.status = :status)
              AND (:fromDate IS NULL OR j.entryDate >= :fromDate)
              AND (:toDate IS NULL OR j.entryDate <= :toDate)
            ORDER BY j.entryDate DESC, j.createdAt DESC
            """)
    Page<JournalEntry> search(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
