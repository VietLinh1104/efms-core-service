package com.linhdv.efms_core_service.controller.accounting;

import com.linhdv.efms_core_service.dto.accounting.response.TrialBalanceResponse;
import com.linhdv.efms_core_service.service.accounting.TrialBalanceService;
import com.linhdv.efms_core_service.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounting/trial-balance")
@RequiredArgsConstructor
@Tag(name = "Trial Balance", description = "Bảng cân đối tài khoản")
public class TrialBalanceController {

    private final TrialBalanceService trialBalanceService;

    @GetMapping
    @Operation(summary = "Lấy Trial Balance theo kỳ hoặc khoảng ngày")
    public ApiResponse<TrialBalanceResponse> get(
            @Parameter(description = "UUID công ty") @RequestParam UUID companyId,
            @Parameter(description = "UUID kỳ kế toán (ưu tiên nếu có)") @RequestParam(required = false) UUID periodId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd) — dùng khi không có periodId") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd) — dùng khi không có periodId") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        TrialBalanceResponse data = periodId != null
                ? trialBalanceService.getByPeriod(companyId, periodId)
                : trialBalanceService.getByDateRange(companyId, fromDate, toDate);

        return ApiResponse.success(data);
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất Trial Balance (Excel / PDF)", description = "TODO: implement export")
    public ApiResponse<String> export(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID periodId,
            @Parameter(description = "Định dạng xuất: excel hoặc pdf") @RequestParam(defaultValue = "excel") String format) {
        // TODO: tích hợp Apache POI / iText để export
        return ApiResponse.success("Export đang được phát triển");
    }
}
