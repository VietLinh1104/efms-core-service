package com.linhdv.efms_core_service.repository.accounting;

import com.linhdv.efms_core_service.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /** Danh sách tài khoản gốc (không có parent) — dùng để build cây */
    @Query("SELECT a FROM Account a WHERE a.company.id = :companyId AND a.parent IS NULL ORDER BY a.code")
    List<Account> findRootAccounts(@Param("companyId") UUID companyId);

    /** Tất cả tài khoản theo công ty */
    List<Account> findByCompanyIdOrderByCode(UUID companyId);

    Page<Account> findByCompanyIdOrderByCode(UUID companyId, Pageable pageable);

    /** Kiểm tra trùng code trong cùng công ty */
    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    /** Tìm theo code trong công ty */
    Optional<Account> findByCompanyIdAndCode(UUID companyId, String code);
}
