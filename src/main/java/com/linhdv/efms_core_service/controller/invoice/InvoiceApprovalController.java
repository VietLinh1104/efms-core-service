package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.service.camunda.TasklistApiClient;
import com.linhdv.efms_core_service.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.linhdv.efms_core_service.dto.common.ApiResponse;

/**
 * Endpoint dành riêng cho hệ thống để duyệt hoặc từ chối hóa đơn.
 * Endpoint này làm trung gian để Frontend (Next.js) không cần chọc trực tiếp
 * vào Camunda.
 */
@RestController
@RequestMapping("/api/core/invoices")
@RequiredArgsConstructor
public class InvoiceApprovalController {

    private final TasklistApiClient tasklistApiClient;
    private final InvoiceService invoiceService;

    @GetMapping("/tasks")
    public ResponseEntity<?> getAllTasks() {
        java.util.List<Map<String, Object>> tasks = tasklistApiClient.searchAllCreatedTasks();
        if (tasks != null) {
            for (Map<String, Object> task : tasks) {
                String processInstanceKey = String.valueOf(task.get("processInstanceKey"));
                var invoice = invoiceService.getByCamundaProcessId(processInstanceKey);
                if (invoice != null) {
                    task.put("invoiceData", invoice);
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Success", tasks));
    }

    @GetMapping("/tasks/{taskId}/invoice")
    public ResponseEntity<?> getInvoiceByTaskId(@PathVariable String taskId) {
        Map<String, Object> task = tasklistApiClient.getTaskInfo(taskId);
        if (task == null || !task.containsKey("processInstanceKey")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Task not found or processInstanceKey missing"));
        }
        String processInstanceKey = String.valueOf(task.get("processInstanceKey"));
        var invoice = invoiceService.getByCamundaProcessId(processInstanceKey);
        if (invoice != null) {
            return ResponseEntity.ok(ApiResponse.success(invoice));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invoice not found for process: " + processInstanceKey));
    }

}
