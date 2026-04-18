package com.linhdv.efms_core_service.controller.finance;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.finance.request.ReconcileMatchRequest;
import com.linhdv.efms_core_service.dto.finance.response.BankTransactionResponse;
import com.linhdv.efms_core_service.dto.finance.response.ReconciliationSummaryResponse;
import com.linhdv.efms_core_service.service.finance.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/finance/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Bank Reconciliation", description = "Đối chiếu số dư NH - Hệ thống")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các giao dịch NH đang đợi ghép / chờ đối chiếu")
    public ApiResponse<List<BankTransactionResponse>> getPendingMatches(
            @RequestParam UUID bankAccountId) {
        return ApiResponse.success(reconciliationService.getUnreconciledTransactions(bankAccountId));
    }

    @PostMapping("/match")
    @Operation(summary = "Ghép thủ công 1 GD ngân hàng với 1 Bút toán trên Hệ thống")
    public ApiResponse<BankTransactionResponse> manualMatch(
            @Valid @RequestBody ReconcileMatchRequest req) {
        return ApiResponse.success("Khớp giao dịch thành công", reconciliationService.match(req));
    }

    @PostMapping("/auto-match")
    @Operation(summary = "Tính năng tự động tìm kiếm và Match hàng loạt các GD có Amount trùng và cùng Time")
    public ApiResponse<List<BankTransactionResponse>> autoMatch(@RequestParam UUID bankAccountId) {
        return 
                ApiResponse.success("Đã chạy Auto-match (Rule-based)", reconciliationService.autoMatch(bankAccountId));
    }

    @PostMapping("/unmatch/{bankTransactionId}")
    @Operation(summary = "Gỡ / Xóa link khớp của giao dịch (Un-reconcile)")
    public ApiResponse<BankTransactionResponse> unmatch(@PathVariable UUID bankTransactionId) {
        return ApiResponse.success("Gỡ đối chiếu thành công", reconciliationService.unmatch(bankTransactionId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Báo cáo Tổng hợp tình trạng Số dư & Giao dịch")
    public ApiResponse<ReconciliationSummaryResponse> getSummary(@RequestParam UUID bankAccountId) {
        return ApiResponse.success(reconciliationService.getSummary(bankAccountId));
    }
}
