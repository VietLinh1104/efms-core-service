package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.service.camunda.TasklistApiClient;
import com.linhdv.efms_core_service.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoint dành riêng cho hệ thống để duyệt hoặc từ chối hóa đơn.
 * Endpoint này làm trung gian để Frontend (Next.js) không cần chọc trực tiếp vào Camunda.
 */
@RestController
@RequestMapping("/api/core/invoices")
@RequiredArgsConstructor
public class InvoiceApprovalController {

    private final TasklistApiClient tasklistApiClient;
    private final InvoiceService invoiceService;

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveInvoice(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload) {
        String procId = invoiceService.getDetail(id).getCamundaProcessId();
        if (procId == null || procId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Hóa đơn chưa được trigger BPMN process (Không có process id)"));
        }

        String comment = (payload != null && payload.containsKey("comment")) ? payload.get("comment") : "Approved via API";
        String taskId = tasklistApiClient.findTaskIdByProcessInstanceKey(procId);
        
        if (taskId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy Task đang chờ duyệt hoặc task đã được xử lý xong"));
        }

        tasklistApiClient.completeTask(taskId, true, comment);
        return ResponseEntity.ok(Map.of("message", "Đã duyệt hóa đơn thành công thông qua Camunda Tasklist"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectInvoice(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload) {
        String procId = invoiceService.getDetail(id).getCamundaProcessId();
        if (procId == null || procId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Hóa đơn chưa được trigger BPMN process"));
        }

        String comment = (payload != null && payload.containsKey("comment")) ? payload.get("comment") : "Rejected via API";
        String taskId = tasklistApiClient.findTaskIdByProcessInstanceKey(procId);
        
        if (taskId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy Task đang chờ duyệt hoặc task đã được xử lý xong"));
        }

        tasklistApiClient.completeTask(taskId, false, comment);
        return ResponseEntity.ok(Map.of("message", "Đã từ chối hóa đơn"));
    }
}
