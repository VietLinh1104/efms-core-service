package com.linhdv.efms_core_service.controller.finance;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.finance.request.CreateBankAccountRequest;
import com.linhdv.efms_core_service.dto.finance.response.BankAccountResponse;
import com.linhdv.efms_core_service.service.finance.BankAccountService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/v1/finance/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Quản lý Tài khoản Ngân hàng (Cash & Bank)")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @Operation(summary = "Danh sách tài khoản ngân hàng")
    public ApiResponse<PagedResponse<BankAccountResponse>> list(
            @RequestParam UUID companyId,
            @Parameter(description = "Loại (checking, savings)") @RequestParam(required = false) String type,
            @Parameter(description = "Từ khoá") @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(bankAccountService.search(companyId, type, search, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết tài khoản ngân hàng")
    public ApiResponse<BankAccountResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(bankAccountService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản ngân hàng")
    public ApiResponse<BankAccountResponse> create(@Valid @RequestBody CreateBankAccountRequest req) {
        return ApiResponse.success("Mở tài khoản ngân hàng thành công", bankAccountService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tài khoản ngân hàng")
    public ApiResponse<BankAccountResponse> update(
            @PathVariable UUID id, @Valid @RequestBody CreateBankAccountRequest req) {
        return ApiResponse.success("Cập nhật thành công", bankAccountService.update(id, req));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Bật/Tắt trạng thái hoạt động tài khoản")
    public ApiResponse<BankAccountResponse> toggleActive(@PathVariable UUID id) {
        return ApiResponse.success("Thay đổi trạng thái thành công", bankAccountService.toggleActive(id));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Lấy Số dư tài khoản hiện tại")
    public ApiResponse<BigDecimal> getBalance(@PathVariable UUID id) {
        BankAccountResponse ba = bankAccountService.getById(id);
        // Tạm thời trả list zero or opening_balance. Sum từ BankTransaction.amount sẽ
        // chuẩn nhất.
        return ApiResponse.success(ba.getOpeningBalance() != null ? ba.getOpeningBalance() : BigDecimal.ZERO);
    }
}
