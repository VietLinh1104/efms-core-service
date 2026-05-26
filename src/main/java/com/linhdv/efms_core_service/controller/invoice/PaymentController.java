package com.linhdv.efms_core_service.controller.invoice;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.invoice.request.CreatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PaymentResponse;
import com.linhdv.efms_core_service.service.invoice.PaymentService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Quản lý Thanh toán (Thu / Chi)")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Danh sách phiếu thanh toán (Thu/Chi)")
    @PreAuthorize("hasAuthority('PAYMENTS:READ')")
    public ApiResponse<PagedResponse<PaymentResponse>> list(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String paymentType, // in / out
            @RequestParam(required = false) UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                paymentService.search(companyId, paymentType, partnerId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết Phiếu thanh toán kèm các khoản phân bổ")
    @PreAuthorize("hasAuthority('PAYMENTS:READ')")
    public ApiResponse<PaymentResponse> getDetail(@PathVariable UUID id) {
        return ApiResponse.success(paymentService.getDetail(id));
    }

    @PostMapping
    @Operation(summary = "Tạo thanh toán mới")
    @PreAuthorize("hasAuthority('PAYMENTS:CREATE')")
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest req) {
        return ApiResponse.success("Lưu thanh toán thành công", paymentService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phiếu thanh toán")
    @PreAuthorize("hasAuthority('PAYMENTS:UPDATE')")
    public ApiResponse<PaymentResponse> update(
            @PathVariable UUID id, @Valid @RequestBody CreatePaymentRequest req) {
        return ApiResponse.success("Cập nhật thành công", paymentService.update(id, req));
    }



    @PostMapping("/{id}/post")
    @Operation(summary = "Ghi sổ bút toán tổng hợp (Post payment → GL)")
    @PreAuthorize("hasAuthority('PAYMENTS:UPDATE')")
    public ApiResponse<PaymentResponse> postPayment(@PathVariable UUID id) {
        return ApiResponse.success("Đã ghi sổ vào General Ledger thành công.", paymentService.post(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá hoàn toàn thanh toán (chỉ khi chưa Post GL)")
    @PreAuthorize("hasAuthority('PAYMENTS:DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        paymentService.delete(id);
        return ApiResponse.success("Xoá thanh toán thành công");
    }
}
