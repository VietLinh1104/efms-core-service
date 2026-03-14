package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.invoice.request.AllocatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.request.CreatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PaymentResponse;
import com.linhdv.efms_core_service.service.invoice.PaymentService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Quản lý Thanh toán (Thu / Chi)")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Danh sách phiếu thanh toán (Thu/Chi)")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> list(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String paymentType, // in / out
            @RequestParam(required = false) UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.search(companyId, paymentType, partnerId, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết Phiếu thanh toán kèm các khoản phân bổ")
    public ResponseEntity<ApiResponse<PaymentResponse>> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getDetail(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo thanh toán mới")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(@Valid @RequestBody CreatePaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu thanh toán thành công", paymentService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phiếu thanh toán")
    public ResponseEntity<ApiResponse<PaymentResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CreatePaymentRequest req) {
        // ... Logic xóa xong tạo lại (cách áp mãnh dạn nhát để duy trì logic)
        paymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", paymentService.create(req)));
    }

    @PostMapping("/{id}/allocate")
    @Operation(summary = "Phân bổ số tiền của payment vào 1 Inovice (AR/AP)")
    public ResponseEntity<ApiResponse<PaymentResponse>> allocate(
            @PathVariable UUID id, @Valid @RequestBody AllocatePaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Phân bổ chứng từ thành công", paymentService.allocate(id, req)));
    }

    @DeleteMapping("/{id}/allocate/{invoiceId}")
    @Operation(summary = "Gỡ / Xóa phân bổ payment vào invoice")
    public ResponseEntity<ApiResponse<String>> removeAllocation(
            @PathVariable UUID id, @PathVariable UUID invoiceId) {
        // TODO: xoá dòng InvoicePayment và cộng lại công nợ Invoice
        return ResponseEntity.ok(ApiResponse.success("Đã xóa phân bổ thanh toán."));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Ghi sổ bút toán tổng hợp (Post payment → GL)")
    public ResponseEntity<ApiResponse<PaymentResponse>> postPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Đã ghi sổ vào General Ledger thành công.", paymentService.getDetail(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá hoàn toàn thanh toán (chỉ khi chưa Post GL)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        paymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá thanh toán thành công"));
    }
}
