package com.linhdv.efms_core_service.controller.report;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.report.response.AgingResponse;
import com.linhdv.efms_core_service.dto.report.response.BalanceSheetResponse;
import com.linhdv.efms_core_service.dto.report.response.CashFlowResponse;
import com.linhdv.efms_core_service.dto.report.response.ProfitLossResponse;
import com.linhdv.efms_core_service.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Các Báo cáo Tài chính Kế toán tổng hợp")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/balance-sheet")
    @Operation(summary = "Bảng Cân đối kế toán (Balance Sheet)")
    public ResponseEntity<ApiResponse<BalanceSheetResponse>> getBalanceSheet(
            @RequestParam UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getBalanceSheet(companyId, asOfDate)));
    }

    @GetMapping("/profit-loss")
    @Operation(summary = "Báo cáo Kết quả Hoạt động Kinh doanh (Profit & Loss / Income Statement)")
    public ResponseEntity<ApiResponse<ProfitLossResponse>> getProfitLoss(
            @RequestParam UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProfitLoss(companyId, fromDate, toDate)));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Báo cáo Lưu chuyển tiền tệ (Cash Flow Statement)")
    public ResponseEntity<ApiResponse<CashFlowResponse>> getCashFlow(
            @RequestParam UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getCashFlowStatement(companyId, fromDate, toDate)));
    }

    @GetMapping("/ar-aging")
    @Operation(summary = "Báo cáo Tuổi nợ Phải Thu (AR Aging)")
    public ResponseEntity<ApiResponse<List<AgingResponse>>> getArAging(
            @RequestParam UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(reportService.getAgingReport(companyId, "AR", date)));
    }

    @GetMapping("/ap-aging")
    @Operation(summary = "Báo cáo Tuổi nợ Phải Trả (AP Aging)")
    public ResponseEntity<ApiResponse<List<AgingResponse>>> getApAging(
            @RequestParam UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(reportService.getAgingReport(companyId, "AP", date)));
    }

    @GetMapping("/general-ledger")
    @Operation(summary = "Sổ cái chi tiết theo tài khoản (Account Ledger)", description = "Sử dụng bộ lọc search trên /v1/accounting/journals")
    public ResponseEntity<ApiResponse<String>> getGeneralLedger() {
        return ResponseEntity.ok(
                ApiResponse.success("Vui lòng truy cập module Sổ Nhật Ký chung (Journal Entries) hoặc bộ lọc account"));
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Bảng Cân đối tài khoản (Trial Balance)", description = "Chuyển hướng dùng Endpoint của Accounting module")
    public ResponseEntity<ApiResponse<String>> getTrialBalanceReport() {
        return ResponseEntity.ok(ApiResponse.success("Đã có endpoint ở: GET /v1/accounting/trial-balance"));
    }

    // Export Endpoints
    @GetMapping({ "/balance-sheet/export", "/profit-loss/export", "/cash-flow/export" })
    @Operation(summary = "Xuất các báo cáo ra định dạng Excel / PDF (Stub)")
    public ResponseEntity<ApiResponse<String>> exportReport() {
        return ResponseEntity.ok(ApiResponse.success("Tính năng xuất Excel/PDF đang được phát triển..."));
    }
}
