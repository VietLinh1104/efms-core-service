package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.invoice.request.CreatePartnerRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.dto.invoice.response.PartnerResponse;
import com.linhdv.efms_core_service.service.invoice.InvoiceService;
import com.linhdv.efms_core_service.service.invoice.PartnerService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/partners")
@RequiredArgsConstructor
@Tag(name = "Partners", description = "Quản lý Đối tác (Khách hàng / Nhà cung cấp)")
public class PartnerController {

    private final PartnerService partnerService;
    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "Danh sách đối tác (phân trang)")
    public ApiResponse<PagedResponse<PartnerResponse>> list(
            @RequestParam UUID companyId,
            @Parameter(description = "Loại (customer/vendor)") @RequestParam(required = false) String type,
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(partnerService.search(companyId, type, search, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết đối tác")
    public ApiResponse<PartnerResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(partnerService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo đối tác mới")
    public ApiResponse<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest req) {
        return ApiResponse.success("Thêm thành công", partnerService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật đối tác")
    public ApiResponse<PartnerResponse> update(
            @PathVariable UUID id, @Valid @RequestBody CreatePartnerRequest req) {
        return ApiResponse.success("Cập nhật thành công", partnerService.update(id, req));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Bật / tắt đối tác")
    public ApiResponse<PartnerResponse> toggleActive(@PathVariable UUID id) {
        return ApiResponse.success(partnerService.toggleActive(id));
    }

    @GetMapping("/{id}/invoices")
    @Operation(summary = "Lịch sử hóa đơn của đối tác")
    public ApiResponse<List<InvoiceResponse>> getPartnerInvoices(@PathVariable UUID id) {
        return ApiResponse.success(invoiceService.getByPartner(id));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Số dư công nợ của đối tác")
    public ApiResponse<BigDecimal> getBalance(@PathVariable UUID id) {
        // Có thể tính từ invoiceService / Payment hoặc trực tiếp từ journalLine cho
        // chuẩn xác:
        // Tạm thời trả về 0 để mô tả khung DTO.
        return ApiResponse.success(BigDecimal.ZERO);
    }
}
