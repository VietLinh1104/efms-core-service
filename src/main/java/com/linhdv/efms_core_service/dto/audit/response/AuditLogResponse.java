package com.linhdv.efms_core_service.dto.audit.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO trả về thông tin audit log — không expose trực tiếp Entity.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {

    private UUID id;

    /** Tên bảng bị thay đổi (ví dụ: "invoices", "payments", "partners") */
    private String tableName;

    /** UUID của bản ghi bị thay đổi */
    private UUID recordId;

    /**
     * Loại hành động:
     * INSERT, UPDATE, DELETE — CRUD cơ bản
     * CONFIRM, APPROVE, REJECT, CANCEL — state transitions của Invoice
     * PAYMENT_POST, PAYMENT_ALLOCATE — hành động đặc biệt của Payment
     */
    private String action;

    /** UUID của user thực hiện thay đổi (từ Identity Service) */
    private UUID changedBy;

    /** Tên của user thực hiện thay đổi (lấy từ Identity Service qua batch fetching) */
    private String changedByName;

    /** Thời điểm thay đổi */
    private Instant changedAt;

    /** Snapshot dữ liệu trước khi thay đổi (null với INSERT) */
    private Map<String, Object> oldData;

    /** Snapshot dữ liệu sau khi thay đổi (null với DELETE) */
    private Map<String, Object> newData;
}
