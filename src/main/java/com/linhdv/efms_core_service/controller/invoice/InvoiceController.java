package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.invoice.request.CreateInvoiceRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.service.invoice.InvoiceService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Quản lý Hóa đơn / Bill (AR / AP)")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "Danh sách hóa đơn (có phân trang và filter)")
    public ApiResponse<PagedResponse<InvoiceResponse>> list(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String invoiceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                invoiceService.search(companyId, invoiceType, status, partnerId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết hóa đơn kèm các dòng lines")
    public ApiResponse<InvoiceResponse> getDetail(@PathVariable UUID id) {
        return ApiResponse.success(invoiceService.getDetail(id));
    }

    @PostMapping
    @Operation(summary = "Tạo Hóa đơn (draft)")
    public ApiResponse<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest req) {
        return ApiResponse.success("Lưu hóa đơn thành công", invoiceService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật Hóa đơn (chỉ khi draft)")
    public ApiResponse<InvoiceResponse> update(
            @PathVariable UUID id, @Valid @RequestBody CreateInvoiceRequest req) {
        invoiceService.delete(id); // Cách tương tự như sửa Journal
        return ApiResponse.success("Cập nhật thành công", invoiceService.create(req));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Xác nhận hóa đơn (draft → open)")
    public ApiResponse<InvoiceResponse> confirm(@PathVariable UUID id) {
        return ApiResponse.success("Xác nhận thành công", invoiceService.confirm(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Duyệt hóa đơn mua hàng (AP) — AP Approve")
    public ApiResponse<InvoiceResponse> approve(@PathVariable UUID id) {
        return ApiResponse.success("Duyệt hóa đơn thành công", invoiceService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Từ chối duyệt hóa đơn (AP) — AP Reject")
    public ApiResponse<InvoiceResponse> reject(@PathVariable UUID id) {
        return ApiResponse.success("Từ chối duyệt hóa đơn", invoiceService.reject(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Huỷ hóa đơn")
    public ApiResponse<InvoiceResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Huỷ hóa đơn thành công", invoiceService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá hoàn toàn hóa đơn (chỉ draft)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        invoiceService.delete(id);
        return ApiResponse.success("Xoá hóa đơn thành công");
    }

    // Các tính năng phân bổ, overdue báo cáo.
    @GetMapping("/overdue")
    @Operation(summary = "Lấy các hóa đơn quá hạn chưa thanh toán (AR/AP)")
    public ApiResponse<List<InvoiceResponse>> getOverdue(@RequestParam UUID companyId) {
        return ApiResponse.success(invoiceService.getOverdue(companyId));
    }

    @GetMapping("/aging")
    @Operation(summary = "Lấy Báo cáo Tuổi nợ AR/AP")
    public ApiResponse<String> getAgingReport(@RequestParam UUID companyId) {
        return ApiResponse.success("Tính năng Đang phát triển. (Tuổi nợ: 0-30, 31-60, 61-90, over)");
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất danh sách Hóa đơn")
    public ApiResponse<String> exportList() {
        return ApiResponse.success("Export đang được phát triển...");
    }
}
