package com.linhdv.efms_core_service.controller;

import com.linhdv.efms_core_service.dto.audit.response.AuditLogResponse;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.service.AuditService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * AuditLogController — cung cấp API xem lịch sử thay đổi dữ liệu tài chính.
 * Tất cả endpoint đều READ-ONLY (không có POST/PUT/DELETE).
 */
@RestController
@RequestMapping("/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Lịch sử thay đổi dữ liệu tài chính (Core Service)")
public class AuditLogController {

    private final AuditService auditService;

    /**
     * Danh sách audit log toàn hệ thống, có phân trang.
     * Lọc theo tên bảng (tuỳ chọn).
     *
     * Ví dụ: GET /v1/audit-logs?tableName=invoices&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "Danh sách audit log (toàn hệ thống, có phân trang)")
    @PreAuthorize("hasAuthority('AUDIT:READ')")
    public ApiResponse<PagedResponse<AuditLogResponse>> listAuditLogs(
            @RequestHeader(name = "X-Company-Id", required = false) UUID companyId,
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(auditService.getAll(tableName, companyId, page, size));
    }

    /**
     * Lịch sử thay đổi của một record cụ thể — dùng cho History Timeline trên UI.
     * Kết quả sắp xếp từ cũ → mới (timeline view).
     *
     * Ví dụ: GET /v1/audit-logs/record?tableName=invoices&recordId={uuid}
     */
    @GetMapping("/record")
    @Operation(summary = "Timeline lịch sử thay đổi của một record cụ thể")
    @PreAuthorize("hasAuthority('AUDIT:READ')")
    public ApiResponse<List<AuditLogResponse>> getRecordHistory(
            @RequestHeader(name = "X-Company-Id", required = false) UUID companyId,
            @RequestParam String tableName,
            @RequestParam UUID recordId) {
        return ApiResponse.success(auditService.getRecordHistory(tableName, recordId, companyId));
    }
}
