package com.linhdv.efms_core_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linhdv.efms_core_service.dto.audit.response.AuditLogResponse;
import com.linhdv.efms_core_service.entity.AuditLog;
import com.linhdv.efms_core_service.repository.AuditLogRepository;
import com.linhdv.efms_core_service.dto.integration.UserBasicInfo;
import com.linhdv.efms_core_service.service.integration.IdentityServiceClient;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuditService — ghi lịch sử thay đổi dữ liệu tài chính vào bảng core.audit_logs.
 *
 * Thiết kế:
 * - @Async("auditExecutor"): ghi audit trên thread riêng, không block main request.
 * - REQUIRES_NEW: transaction độc lập — audit KHÔNG bị rollback dù main TX thất bại
 *   sau khi gọi audit (ví dụ: lỗi sau bước save entity).
 * - Lấy changedBy từ SecurityContextHolder.details (set bởi GatewayHeaderFilter).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final IdentityServiceClient identityServiceClient;

    // ── Ghi Log ───────────────────────────────────────────────────────────────

    /**
     * Ghi một audit log entry. Được gọi sau khi entity đã được save thành công.
     *
     * @param tableName Tên bảng DB (vd: "invoices", "payments", "partners")
     * @param recordId  UUID của bản ghi bị thay đổi
     * @param action    Loại hành động (INSERT, UPDATE, DELETE, CONFIRM, APPROVE,
     *                  REJECT, CANCEL, PAYMENT_POST, PAYMENT_ALLOCATE)
     * @param oldData   Snapshot trước thay đổi — null cho INSERT
     * @param newData   Snapshot sau thay đổi — null cho DELETE
     */
    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String tableName, UUID recordId, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTableName(tableName);
            entry.setRecordId(recordId);
            entry.setAction(action);
            entry.setChangedBy(resolveCurrentUserId());
            entry.setChangedAt(Instant.now());
            entry.setOldData(oldData);
            entry.setNewData(newData);

            auditLogRepository.save(entry);
            log.debug("[AUDIT] {} on {} id={}", action, tableName, recordId);
        } catch (Exception e) {
            // Audit thất bại không được ảnh hưởng main flow
            log.error("[AUDIT] Không thể ghi audit log: table={}, id={}, action={} — {}",
                    tableName, recordId, action, e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Lịch sử thay đổi của một record cụ thể (sắp xếp cũ → mới để hiển thị
     * timeline).
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecordHistory(String tableName, UUID recordId, UUID companyId) {
        List<AuditLogResponse> responses = auditLogRepository.findByTableNameAndRecordIdOrderByChangedAtAsc(tableName, recordId)
                .stream().map(this::toResponse).toList();
        enrichUserNames(responses, companyId);
        return responses;
    }

    /**
     * Danh sách audit log toàn hệ thống có phân trang, lọc theo tableName tuỳ
     * chọn.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAll(String tableName, UUID companyId, int page, int size) {
        Page<AuditLog> data = auditLogRepository.findAllFiltered(tableName, PageRequest.of(page, size));
        List<AuditLogResponse> responses = data.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        enrichUserNames(responses, companyId);
        return PagedResponse.of(responses, page, size, data.getTotalElements());
    }

    private void enrichUserNames(List<AuditLogResponse> responses, UUID companyId) {
        if (responses == null || responses.isEmpty() || companyId == null) {
            return;
        }

        Set<UUID> userIds = responses.stream()
                .map(AuditLogResponse::getChangedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return;
        }

        Map<UUID, UserBasicInfo> userMap = identityServiceClient.getBatchUsers(userIds, companyId);

        for (AuditLogResponse res : responses) {
            if (res.getChangedBy() != null) {
                UserBasicInfo user = userMap.get(res.getChangedBy());
                if (user != null) {
                    res.setChangedByName(user.getFullName());
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Chuyển entity object sang Map<String, Object> để lưu vào JSONB.
     * Trả về null nếu entity là null (dùng cho INSERT/DELETE).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(Object entity) {
        if (entity == null) return null;
        try {
            return objectMapper.convertValue(entity, Map.class);
        } catch (Exception e) {
            log.warn("[AUDIT] Không thể serialize entity sang Map: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Đọc userId từ SecurityContextHolder.details (được set bởi
     * GatewayHeaderFilter).
     * Trả về null nếu không có authentication (gọi từ worker thread nội bộ).
     */
    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object details = auth.getDetails();
        if (details instanceof String str && !str.isBlank()) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException e) {
                log.warn("[AUDIT] X-User-Id không phải UUID hợp lệ: {}", str);
            }
        }
        return null;
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .tableName(log.getTableName())
                .recordId(log.getRecordId())
                .action(log.getAction())
                .changedBy(log.getChangedBy())
                .changedAt(log.getChangedAt())
                .oldData(log.getOldData())
                .newData(log.getNewData())
                .build();
    }
}
