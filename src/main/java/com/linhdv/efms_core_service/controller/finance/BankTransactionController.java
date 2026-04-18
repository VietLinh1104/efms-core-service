package com.linhdv.efms_core_service.controller.finance;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.finance.request.CreateBankTransactionRequest;
import com.linhdv.efms_core_service.dto.finance.response.BankTransactionResponse;
import com.linhdv.efms_core_service.service.finance.BankTransactionService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/v1/finance/bank-transactions")
@RequiredArgsConstructor
@Tag(name = "Bank Transactions", description = "Quản lý Giao dịch Ngân hàng (Thủ công / Import)")
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    @GetMapping
    @Operation(summary = "Danh sách giao dịch ngân hàng")
    public ApiResponse<PagedResponse<BankTransactionResponse>> list(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID bankAccountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status, // reconciled / unreconciled
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                bankTransactionService.search(companyId, bankAccountId, type, status, fromDate, toDate, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một giao dịch ngân hàng")
    public ApiResponse<BankTransactionResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(bankTransactionService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo một giao dịch thủ công trên hệ thống")
    public ApiResponse<BankTransactionResponse> create(
            @Valid @RequestBody CreateBankTransactionRequest req) {
        return ApiResponse.success("Thêm giao dịch thủ công thành công", bankTransactionService.create(req));
    }

    @PostMapping("/import")
    @Operation(summary = "Import bản sao kê (Bank Statement) từ file CSV/Excel", description = "Đang phát triển - TODO: sử dụng MultiPartFile upload")
    public ApiResponse<String> importData() {
        return ApiResponse.success("import đang được phát triển...");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá giao dịch (Chỉ khi CHƯA được đối chiếu - unreconciled)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        bankTransactionService.delete(id);
        return ApiResponse.success("Xoá giao dịch thành công");
    }
}
