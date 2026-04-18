package com.linhdv.efms_core_service.controller.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateFiscalPeriodRequest;
import com.linhdv.efms_core_service.dto.accounting.response.FiscalPeriodResponse;
import com.linhdv.efms_core_service.service.accounting.FiscalPeriodService;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounting/fiscal-periods")
@RequiredArgsConstructor
@Tag(name = "Fiscal Periods", description = "Quản lý kỳ kế toán")
public class FiscalPeriodController {

    private final FiscalPeriodService fiscalPeriodService;

    @GetMapping
    @Operation(summary = "Danh sách kỳ kế toán")
    public ApiResponse<List<FiscalPeriodResponse>> list(
            @Parameter(description = "UUID công ty") @RequestParam UUID companyId) {
        return ApiResponse.success(fiscalPeriodService.list(companyId));
    }

    @PostMapping
    @Operation(summary = "Tạo kỳ kế toán mới")
    public ApiResponse<FiscalPeriodResponse> create(
            @Valid @RequestBody CreateFiscalPeriodRequest req) {
        return ApiResponse.success("Tạo kỳ kế toán thành công", fiscalPeriodService.create(req));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Đóng kỳ kế toán")
    public ApiResponse<FiscalPeriodResponse> close(
            @Parameter(description = "UUID kỳ kế toán") @PathVariable UUID id) {
        return ApiResponse.success("Đóng kỳ kế toán thành công", fiscalPeriodService.close(id));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Mở lại kỳ kế toán (Admin)")
    public ApiResponse<FiscalPeriodResponse> reopen(
            @Parameter(description = "UUID kỳ kế toán") @PathVariable UUID id) {
        return ApiResponse.success("Mở lại kỳ kế toán thành công", fiscalPeriodService.reopen(id));
    }
}
