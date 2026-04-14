package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.wrapper.PagedResponse;

/**
 * Endpoint dành riêng cho hệ thống để duyệt hoặc từ chối hóa đơn.
 * Endpoint này làm trung gian để Frontend (Next.js) không cần chọc trực tiếp
 * vào Camunda.
 */
@RestController
@RequestMapping("/api/core/invoices")
@RequiredArgsConstructor
public class InvoiceApprovalController {
    private final InvoiceService invoiceService;

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Success", invoiceService.getAllApprovalTasks(page, size)));
    }

    @GetMapping("/tasks/{taskId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByTaskId(@PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceTaskDetail(taskId)));
    }

}
