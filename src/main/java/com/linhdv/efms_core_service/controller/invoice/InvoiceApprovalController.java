package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.service.invoice.InvoiceService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller quản lý quy trình phê duyệt AP Bill.
 * Không còn phụ thuộc Camunda — trạng thái lưu trực tiếp vào DB.
 */
@RestController
@RequestMapping("/v1/invoice-tasks")
@RequiredArgsConstructor
@Tag(name = "Invoice Approval", description = "Phê duyệt AP Bill — quản lý trạng thái trực tiếp trên DB")
public class InvoiceApprovalController {

    private final InvoiceService invoiceService;

    /**
     * Danh sách AP Bill đang chờ phê duyệt (approval_status = pending).
     */
    @GetMapping("/tasks")
    @Operation(summary = "Danh sách AP Bill đang chờ phê duyệt")
    @PreAuthorize("hasAuthority('INVOICE:READ')")
    public ApiResponse<PagedResponse<InvoiceResponse>> getPendingApprovals(
            @RequestParam UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Success", invoiceService.getPendingApprovals(companyId, page, size));
    }

    /**
     * Chi tiết một hóa đơn AP đang chờ duyệt (dùng invoiceId trực tiếp).
     */
    @GetMapping("/tasks/{invoiceId}/invoice")
    @Operation(summary = "Chi tiết hóa đơn AP đang chờ duyệt")
    @PreAuthorize("hasAuthority('INVOICE:READ')")
    public ApiResponse<InvoiceResponse> getInvoiceDetail(@PathVariable UUID invoiceId) {
        return ApiResponse.success(invoiceService.getDetail(invoiceId));
    }
}
