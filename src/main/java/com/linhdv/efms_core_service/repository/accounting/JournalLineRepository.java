package com.linhdv.efms_core_service.repository.accounting;

import com.linhdv.efms_core_service.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    /** Tất cả dòng bút toán của một chứng từ */
    List<JournalLine> findByJournalEntryIdOrderByCreatedAt(UUID journalEntryId);

    /**
     * Tổng hợp phát sinh Nợ/Có theo tài khoản trong khoảng ngày (dùng cho Trial
     * Balance)
     */
    @Query("""
            SELECT jl.account.id,
                   SUM(jl.debit)  AS totalDebit,
                   SUM(jl.credit) AS totalCredit
            FROM JournalLine jl
            JOIN jl.journalEntry je
            WHERE je.companyId = :companyId
              AND je.status = 'posted'
              AND je.entryDate BETWEEN :fromDate AND :toDate
            GROUP BY jl.account.id
            """)
    List<Object[]> aggregateByAccount(
            @Param("companyId") UUID companyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
