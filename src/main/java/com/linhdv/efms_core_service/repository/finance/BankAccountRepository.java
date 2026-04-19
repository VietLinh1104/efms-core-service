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

    @Query(value = """
            SELECT * FROM public.bank_accounts b
            WHERE b.company_id = :companyId
              AND (:type IS NULL OR b.type = CAST(:type AS VARCHAR))
              AND (
                :search IS NULL
                OR lower(b.name) LIKE lower(CAST(CONCAT('%', :search, '%') AS VARCHAR))
                OR lower(b.account_number) LIKE lower(CAST(CONCAT('%', :search, '%') AS VARCHAR))
              )
            ORDER BY b.created_at DESC
            """, nativeQuery = true)
    Page<BankAccount> search(
            @Param("companyId") UUID companyId,
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable
    );
}
