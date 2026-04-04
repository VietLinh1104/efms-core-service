package com.linhdv.efms_core_service.repository.finance;

import com.linhdv.efms_core_service.entity.BankTransaction;
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
public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    @Query("""
            SELECT bt FROM BankTransaction bt
            JOIN bt.bankAccount ba
            WHERE ba.companyId = :companyId
              AND (:accountId IS NULL OR ba.id = :accountId)
              AND (:type IS NULL OR bt.type = :type)
              AND (:status IS NULL OR
                  (:status = 'reconciled' AND bt.isReconciled = true) OR
                  (:status = 'unreconciled' AND bt.isReconciled = false))
              AND (:fromDate IS NULL OR bt.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR bt.transactionDate <= :toDate)
            ORDER BY bt.transactionDate DESC, bt.createdAt DESC
            """)
    Page<BankTransaction> search(
            @Param("companyId") UUID companyId,
            @Param("accountId") UUID accountId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    // Lấy các giao dịch chưa đối chiếu của 1 tài khoản
    List<BankTransaction> findByBankAccount_IdAndIsReconciledFalseOrderByTransactionDateAsc(UUID accountId);
    
    // Auto-match có thể tìm theo Amount và Reference hoặc Date.
    // Tạm chưa cần JPA Query cho Auto-match ở mức Repo, có thể viết logic ở Service.
}
