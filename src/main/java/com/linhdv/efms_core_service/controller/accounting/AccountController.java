package com.linhdv.efms_core_service.controller.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateAccountRequest;
import com.linhdv.efms_core_service.dto.accounting.response.AccountBalanceResponse;
import com.linhdv.efms_core_service.dto.accounting.response.AccountResponse;
import com.linhdv.efms_core_service.service.accounting.AccountService;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Quản lý tài khoản kế toán")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Danh sách tài khoản", description = "Lấy danh sách tài khoản theo công ty. Truyền tree=true để lấy dạng cây.")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> list(
            @Parameter(description = "UUID công ty") @RequestParam UUID companyId,
            @Parameter(description = "Trả về dạng cây nếu true") @RequestParam(defaultValue = "false") boolean tree) {
        List<AccountResponse> data = tree
                ? accountService.listTree(companyId)
                : accountService.listAll(companyId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(
            @Parameter(description = "UUID tài khoản") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản mới")
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản thành công", accountService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> update(
            @Parameter(description = "UUID tài khoản") @PathVariable UUID id,
            @Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", accountService.update(id, req)));
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Bật / tắt trạng thái tài khoản")
    public ResponseEntity<ApiResponse<AccountResponse>> toggleActive(
            @Parameter(description = "UUID tài khoản") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.toggleActive(id)));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Số dư tài khoản theo kỳ hoặc khoảng ngày")
    public ResponseEntity<ApiResponse<AccountBalanceResponse>> getBalance(
            @Parameter(description = "UUID tài khoản") @PathVariable UUID id,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getBalance(id, fromDate, toDate)));
    }
}
