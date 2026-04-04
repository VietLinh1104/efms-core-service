package com.linhdv.efms_core_service.repository.finance;

import com.linhdv.efms_core_service.entity.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    @Query("""
            SELECT b FROM BankAccount b
            WHERE b.companyId = :companyId
              AND (:type IS NULL OR b.type = :type)
              AND (:search IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.accountNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY b.createdAt DESC
            """)
    Page<BankAccount> search(
            @Param("companyId") UUID companyId,
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable
    );
}
