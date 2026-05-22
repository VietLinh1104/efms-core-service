package com.linhdv.efms_core_service.service;

import com.linhdv.efms_core_service.dto.dashboard.DashboardSummaryResponse;
import com.linhdv.efms_core_service.dto.dashboard.DashboardSummaryResponse.*;
import com.linhdv.efms_core_service.entity.Invoice;
import com.linhdv.efms_core_service.entity.Payment;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import com.linhdv.efms_core_service.repository.invoice.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DashboardService — Tổng hợp dữ liệu cho trang HomePage.
 * Gom nhiều queries thành 1 response duy nhất để tránh N+1 requests từ Frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UUID companyId) {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1);

        return DashboardSummaryResponse.builder()
                .kpi(buildKpi(companyId, firstOfMonth, today))
                .invoiceStatusStats(buildInvoiceStatusStats(companyId))
                .monthlyFlow(buildMonthlyFlow(companyId, sixMonthsAgo))
                .pendingInvoices(buildPendingInvoices(companyId))
                .recentPayments(buildRecentPayments(companyId))
                .build();
    }

    // ── KPI ──────────────────────────────────────────────────────────────────

    private KpiStats buildKpi(UUID companyId, LocalDate firstOfMonth, LocalDate today) {
        BigDecimal totalAr = invoiceRepository.sumOpenAr(companyId);
        BigDecimal totalAp = invoiceRepository.sumOpenAp(companyId);
        BigDecimal paymentsThisMonth = paymentRepository.sumPaymentInPeriod(companyId, firstOfMonth, today);

        // Đếm AP pending approval
        long pendingApCount = invoiceRepository.countByStatusAndApprovalStatus(companyId)
                .stream()
                .filter(row -> "open".equals(row[0]) && "pending".equals(row[1]))
                .mapToLong(row -> ((Number) row[2]).longValue())
                .sum();

        return KpiStats.builder()
                .totalAr(totalAr != null ? totalAr : BigDecimal.ZERO)
                .totalAp(totalAp != null ? totalAp : BigDecimal.ZERO)
                .paymentsThisMonth(paymentsThisMonth != null ? paymentsThisMonth : BigDecimal.ZERO)
                .pendingApprovalCount(pendingApCount)
                .build();
    }

    // ── Invoice Status Pie Chart ──────────────────────────────────────────────

    private List<InvoiceStatusStat> buildInvoiceStatusStats(UUID companyId) {
        List<Object[]> rows = invoiceRepository.countByStatusAndApprovalStatus(companyId);

        // Gom nhóm theo logic nghiệp vụ → 4 nhóm: Đã duyệt, Chờ duyệt, Từ chối, Nháp
        long approved = 0, pending = 0, rejected = 0, draft = 0;

        for (Object[] row : rows) {
            String status = (String) row[0];
            String approvalStatus = row[1] != null ? (String) row[1] : "";
            long count = ((Number) row[2]).longValue();

            if ("draft".equals(status) || "cancelled".equals(status)) {
                draft += count;
            } else if ("approved".equals(approvalStatus)) {
                approved += count;
            } else if ("rejected".equals(approvalStatus)) {
                rejected += count;
            } else {
                // open/in_payment/paid với pending hoặc AR (không cần approval)
                pending += count;
            }
        }

        List<InvoiceStatusStat> stats = new ArrayList<>();
        if (approved > 0) stats.add(InvoiceStatusStat.builder().name("Đã duyệt").value(approved).color("#22c55e").build());
        if (pending > 0)  stats.add(InvoiceStatusStat.builder().name("Chờ duyệt").value(pending).color("#f59e0b").build());
        if (rejected > 0) stats.add(InvoiceStatusStat.builder().name("Từ chối").value(rejected).color("#ef4444").build());
        if (draft > 0)    stats.add(InvoiceStatusStat.builder().name("Nháp").value(draft).color("#64748b").build());

        return stats;
    }

    // ── Monthly Bar Chart ─────────────────────────────────────────────────────

    private List<MonthlyFlowStat> buildMonthlyFlow(UUID companyId, LocalDate fromDate) {
        List<Object[]> rows = invoiceRepository.sumByMonthAndType(companyId, fromDate);

        // Tạo map 6 tháng gần nhất, đảm bảo đủ tháng kể cả tháng không có dữ liệu
        LinkedHashMap<String, MonthlyFlowStat.MonthlyFlowStatBuilder> map = new LinkedHashMap<>();
        LocalDate cursor = fromDate;
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            String key = String.format("Th%02d", cursor.getMonthValue());
            map.put(cursor.getYear() + "-" + cursor.getMonthValue(),
                    MonthlyFlowStat.builder().month(key).revenue(BigDecimal.ZERO).expense(BigDecimal.ZERO));
            cursor = cursor.plusMonths(1);
        }

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String type = (String) row[2];
            BigDecimal amount = (BigDecimal) row[3];

            // Chuyển về triệu VND
            BigDecimal amountInMillion = amount.divide(MILLION, 2, RoundingMode.HALF_UP);

            String mapKey = year + "-" + month;
            MonthlyFlowStat.MonthlyFlowStatBuilder builder = map.get(mapKey);
            if (builder != null) {
                if ("AR".equals(type)) builder.revenue(amountInMillion);
                else if ("AP".equals(type)) builder.expense(amountInMillion);
            }
        }

        return map.values().stream().map(MonthlyFlowStat.MonthlyFlowStatBuilder::build).toList();
    }

    // ── Pending Invoices Table ────────────────────────────────────────────────

    private List<PendingInvoiceItem> buildPendingInvoices(UUID companyId) {
        List<Invoice> invoices = invoiceRepository.findPendingForDashboard(
                companyId, PageRequest.of(0, 5));

        return invoices.stream().map(inv -> PendingInvoiceItem.builder()
                .id(inv.getId().toString())
                .invoiceNumber(inv.getInvoiceNumber())
                .partnerName(inv.getPartner() != null ? inv.getPartner().getName() : "—")
                .invoiceType(inv.getInvoiceType())
                .totalAmount(inv.getTotalAmount())
                .invoiceDate(inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : null)
                .status(inv.getStatus())
                .approvalStatus(inv.getApprovalStatus())
                .build()
        ).toList();
    }

    // ── Recent Payments ───────────────────────────────────────────────────────

    private List<RecentPaymentItem> buildRecentPayments(UUID companyId) {
        List<Payment> payments = paymentRepository.findRecentForDashboard(
                companyId, PageRequest.of(0, 5));

        return payments.stream().map(p -> RecentPaymentItem.builder()
                .id(p.getId().toString())
                .partnerName(p.getPartner() != null ? p.getPartner().getName() : "—")
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate() != null ? p.getPaymentDate().toString() : null)
                .posted(p.getJournalEntry() != null)
                .build()
        ).toList();
    }
}
