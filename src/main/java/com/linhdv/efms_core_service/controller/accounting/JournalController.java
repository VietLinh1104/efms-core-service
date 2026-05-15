package com.linhdv.efms_core_service.controller.accounting;

import com.linhdv.efms_core_service.dto.accounting.response.JournalEntryResponse;
import com.linhdv.efms_core_service.service.accounting.JournalService;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Journal Controller — Read-only (Đồ án scope).
 * Chứng từ được tự động sinh bởi hệ thống khi Hóa đơn được duyệt hoặc Thanh toán được Post.
 * Không cho phép tạo/sửa/xóa chứng từ thủ công.
 */
@RestController
@RequestMapping("/v1/accounting/journals")
@RequiredArgsConstructor
@Tag(name = "Journal Entries", description = "Danh sách bút toán kế toán (Read-only)")
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    @Operation(summary = "Danh sách bút toán (phân trang, lọc theo trạng thái / ngày)")
    public ApiResponse<PagedResponse<JournalEntryResponse>> list(
            @Parameter(description = "UUID công ty") @RequestParam UUID companyId,
            @Parameter(description = "Lọc theo trạng thái: draft, posted, cancelled") @RequestParam(required = false) String status,
            @Parameter(description = "Từ ngày (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Đến ngày (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Trang hiện tại (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử mỗi trang") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                journalService.list(companyId, status, fromDate, toDate, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết bút toán kèm các dòng Nợ/Có")
    public ApiResponse<JournalEntryResponse> getDetail(
            @Parameter(description = "UUID bút toán") @PathVariable UUID id) {
        return ApiResponse.success(journalService.getDetail(id));
    }
}
