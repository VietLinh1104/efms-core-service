package com.linhdv.efms_core_service.controller;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.dashboard.DashboardSummaryResponse;
import com.linhdv.efms_core_service.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API tổng hợp dữ liệu cho trang Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/core/v1/dashboard/summary?companyId={uuid}
     *
     * Trả về toàn bộ dữ liệu cần thiết cho trang HomePage của EFMS React:
     * - KPI: Tổng AR, AP, Payment tháng này, Số lượng chờ duyệt
     * - Invoice Status Stats: Phân bổ hóa đơn theo trạng thái (Pie Chart)
     * - Monthly Flow: Doanh thu & Chi phí 6 tháng gần nhất (Bar Chart)
     * - Pending Invoices: 5 hóa đơn cần xử lý gần nhất
     * - Recent Payments: 5 thanh toán gần nhất
     */
    @GetMapping("/summary")
    @Operation(summary = "Tổng hợp dữ liệu Dashboard")
    @PreAuthorize("hasAuthority('INVOICE:READ')")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam UUID companyId) {
        return ApiResponse.success(dashboardService.getSummary(companyId));
    }
}
