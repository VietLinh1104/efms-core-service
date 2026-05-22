package com.linhdv.efms_core_service.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO tổng hợp dữ liệu Dashboard — trả về dưới 1 request duy nhất cho trang HomePage.
 */
@Data
@Builder
public class DashboardSummaryResponse {

    /** KPI — Các chỉ số tổng quan */
    private KpiStats kpi;

    /** Phân bổ hóa đơn theo trạng thái (dùng cho Pie Chart) */
    private List<InvoiceStatusStat> invoiceStatusStats;

    /** Doanh thu & chi phí tổng hợp theo tháng (dùng cho Bar Chart, 6 tháng gần nhất) */
    private List<MonthlyFlowStat> monthlyFlow;

    /** Danh sách hóa đơn chờ xử lý (top 5) */
    private List<PendingInvoiceItem> pendingInvoices;

    /** Thanh toán gần đây nhất (top 5) */
    private List<RecentPaymentItem> recentPayments;

    // ── Nested Types ──────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class KpiStats {
        /** Tổng công nợ phải thu (AR) — tổng totalAmount của các AR invoice status=open/in_payment */
        private BigDecimal totalAr;
        /** Tổng công nợ phải trả (AP) — tổng totalAmount của các AP invoice status=open/in_payment */
        private BigDecimal totalAp;
        /** Tổng tiền thanh toán trong tháng hiện tại */
        private BigDecimal paymentsThisMonth;
        /** Tổng số hóa đơn đang chờ phê duyệt (AP, approval_status=pending) */
        private long pendingApprovalCount;
    }

    @Data
    @Builder
    public static class InvoiceStatusStat {
        /** Nhãn trạng thái (VD: "Đã duyệt", "Chờ duyệt", "Từ chối", "Nháp") */
        private String name;
        /** Số lượng hóa đơn */
        private long value;
        /** Màu sắc hex cho biểu đồ */
        private String color;
    }

    @Data
    @Builder
    public static class MonthlyFlowStat {
        /** Tháng theo định dạng "Th01", "Th02"... */
        private String month;
        /** Tổng doanh thu AR (triệu VND) */
        private BigDecimal revenue;
        /** Tổng chi phí AP (triệu VND) */
        private BigDecimal expense;
    }

    @Data
    @Builder
    public static class PendingInvoiceItem {
        private String id;
        private String invoiceNumber;
        private String partnerName;
        private String invoiceType;
        private BigDecimal totalAmount;
        private String invoiceDate;
        private String status;
        private String approvalStatus;
    }

    @Data
    @Builder
    public static class RecentPaymentItem {
        private String id;
        private String partnerName;
        private BigDecimal amount;
        private String paymentDate;
        /** true nếu payment đã được post lên sổ cái */
        private boolean posted;
    }
}
