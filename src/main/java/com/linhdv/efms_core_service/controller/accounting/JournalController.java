package com.linhdv.efms_core_service.controller.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateJournalRequest;
import com.linhdv.efms_core_service.dto.accounting.response.JournalEntryResponse;
import com.linhdv.efms_core_service.service.accounting.JournalService;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/journals")
@RequiredArgsConstructor
@Tag(name = "Journal Entries", description = "Quản lý chứng từ kế toán")
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    @Operation(summary = "Danh sách chứng từ (có phân trang, lọc theo trạng thái / ngày)")
    public ResponseEntity<ApiResponse<PagedResponse<JournalEntryResponse>>> list(
            @Parameter(description = "UUID công ty") @RequestParam UUID companyId,
            @Parameter(description = "Lọc theo trạng thái: draft, posted, cancelled") @RequestParam(required = false) String status,
            @Parameter(description = "Từ ngày (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Đến ngày (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Trang hiện tại (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số phần tử mỗi trang") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                journalService.list(companyId, status, fromDate, toDate, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết chứng từ kèm các dòng bút toán")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> getDetail(
            @Parameter(description = "UUID chứng từ") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(journalService.getDetail(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo chứng từ mới (trạng thái draft)", description = "Tổng Nợ phải bằng Tổng Có mới tạo được")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> create(
            @Valid @RequestBody CreateJournalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chứng từ thành công", journalService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật chứng từ (chỉ áp dụng khi ở trạng thái draft)")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> update(
            @Parameter(description = "UUID chứng từ") @PathVariable UUID id,
            @Valid @RequestBody CreateJournalRequest req) {
        // Xoá lines cũ, tạo lại — dùng JournalService
        journalService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", journalService.create(req)));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post chứng từ (draft → posted)")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> post(
            @Parameter(description = "UUID chứng từ") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Post chứng từ thành công", journalService.post(id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Huỷ chứng từ")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> cancel(
            @Parameter(description = "UUID chứng từ") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Huỷ chứng từ thành công", journalService.cancel(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá chứng từ (chỉ áp dụng khi ở trạng thái draft)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "UUID chứng từ") @PathVariable UUID id) {
        journalService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá chứng từ thành công"));
    }
}
