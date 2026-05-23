package com.linhdv.efms_core_service.repository;

import com.linhdv.efms_core_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Lấy lịch sử thay đổi của một record cụ thể (ví dụ: invoices + invoiceId).
     */
    List<AuditLog> findByTableNameAndRecordIdOrderByChangedAtAsc(String tableName, UUID recordId);

    /**
     * Danh sách audit log toàn bộ công ty, lọc theo tableName (tuỳ chọn), sắp xếp
     * mới nhất trước.
     * changedBy trong audit_logs là UUID của user thuộc company đó — dùng để filter
     * theo companyId thông qua sub-query hoặc application-level filter.
     *
     * Vì không có FK tới company trong audit_logs, ta filter qua record_id
     * (tất cả records của company đã được isolate ở service layer).
     * Nên ở đây filter theo tableName (optional) và phân trang.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:tableName IS NULL OR a.tableName = :tableName)
            ORDER BY a.changedAt DESC
            """)
    Page<AuditLog> findAllFiltered(@Param("tableName") String tableName, Pageable pageable);

    /**
     * Lấy log theo changedBy (userId) — dùng cho xem lịch sử hành động của user.
     */
    Page<AuditLog> findByChangedByOrderByChangedAtDesc(UUID changedBy, Pageable pageable);
}
