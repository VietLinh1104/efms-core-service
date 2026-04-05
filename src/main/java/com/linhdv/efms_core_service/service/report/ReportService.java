package com.linhdv.efms_core_service.service.report;

import com.linhdv.efms_core_service.dto.report.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {
    @Transactional(readOnly = true)
    public BalanceSheetResponse getBalanceSheet(UUID companyId, LocalDate asOfDate) {
        // Mock data để thể hiện cấu trúc Bảng cân đối kế toán
        ReportRowResponse cash = new ReportRowResponse("111 - Tiền mặt", new BigDecimal("50000000"), 1, false, null);
        ReportRowResponse bank = new ReportRowResponse("112 - Tiền gửi ngân hàng", new BigDecimal("150000000"), 1,
                false, null);
        List<ReportRowResponse> currentAssets = List.of(cash, bank);
        ReportRowResponse assets = new ReportRowResponse("TỔNG TÀI SẢN", new BigDecimal("200000000"), 0, true,
                currentAssets);

        ReportRowResponse payable = new ReportRowResponse("331 - Phải trả người bán", new BigDecimal("30000000"), 1,
                false, null);
        List<ReportRowResponse> liabList = List.of(payable);
        ReportRowResponse liability = new ReportRowResponse("NỢ PHẢI TRẢ", new BigDecimal("30000000"), 0, true,
                liabList);

        ReportRowResponse capital = new ReportRowResponse("411 - Vốn đầu tư của CSH", new BigDecimal("170000000"), 1,
                false, null);
        List<ReportRowResponse> equityList = List.of(capital);
        ReportRowResponse equity = new ReportRowResponse("VỐN CHỦ SỞ HỮU", new BigDecimal("170000000"), 0, true,
                equityList);

        return BalanceSheetResponse.builder()
                .companyName("Công ty Demo (ID: " + companyId + ")")
                .asOfDate(asOfDate)
                .assets(List.of(assets))
                .liabilities(List.of(liability))
                .equity(List.of(equity))
                .build();
    }

    @Transactional(readOnly = true)
    public ProfitLossResponse getProfitLoss(UUID companyId, LocalDate fromDate, LocalDate toDate) {
        // Mock data Kết quả kinh doanh
        ReportRowResponse rev1 = new ReportRowResponse("511 - Doanh thu bán hàng", new BigDecimal("800000000"), 1,
                false, null);
        ReportRowResponse revenues = new ReportRowResponse("TỔNG DOANH THU", new BigDecimal("800000000"), 0, true,
                List.of(rev1));

        ReportRowResponse exp1 = new ReportRowResponse("632 - Giá vốn hàng bán", new BigDecimal("500000000"), 1, false,
                null);
        ReportRowResponse exp2 = new ReportRowResponse("642 - Chi phí quản lý", new BigDecimal("150000000"), 1, false,
                null);
        ReportRowResponse expenses = new ReportRowResponse("TỔNG CHI PHÍ", new BigDecimal("650000000"), 0, true,
                List.of(exp1, exp2));

        ReportRowResponse netIncome = new ReportRowResponse("LỢI NHUẬN RÒNG (LÃI/LỖ TIẾP THEO)",
                new BigDecimal("150000000"), 0, true, null);

        return ProfitLossResponse.builder()
                .companyName("Công ty Demo (ID: " + companyId + ")")
                .fromDate(fromDate)
                .toDate(toDate)
                .revenues(List.of(revenues))
                .expenses(List.of(expenses))
                .netIncome(netIncome)
                .build();
    }

    @Transactional(readOnly = true)
    public CashFlowResponse getCashFlowStatement(UUID companyId, LocalDate fromDate, LocalDate toDate) {
        // Mock data Lưu chuyển tiền tệ
        ReportRowResponse op1 = new ReportRowResponse("Tiền thu từ bán hàng", new BigDecimal("800000000"), 1, false,
                null);
        ReportRowResponse op2 = new ReportRowResponse("Tiền chi trả người cung cấp", new BigDecimal("-400000000"), 1,
                false, null);
        ReportRowResponse ops = new ReportRowResponse("Lưu chuyển tiền thuần từ hoạt động kinh doanh",
                new BigDecimal("400000000"), 0, true, List.of(op1, op2));

        ReportRowResponse inv1 = new ReportRowResponse("Mua sắm TSCĐ", new BigDecimal("-100000000"), 1, false, null);
        ReportRowResponse invs = new ReportRowResponse("Lưu chuyển tiền thuần từ hoạt động đầu tư",
                new BigDecimal("-100000000"), 0, true, List.of(inv1));

        ReportRowResponse fins = new ReportRowResponse("Lưu chuyển tiền thuần từ hoạt động tài chính",
                new BigDecimal("0"), 0, true, new ArrayList<>());

        return CashFlowResponse.builder()
                .companyName("Công ty Demo (ID: " + companyId + ")")
                .fromDate(fromDate)
                .toDate(toDate)
                .operatingActivities(List.of(ops))
                .investingActivities(List.of(invs))
                .financingActivities(List.of(fins))
                .netCashFlow(new ReportRowResponse("LƯU CHUYỂN TIỀN THUẦN TRONG KỲ", new BigDecimal("300000000"), 0,
                        true, null))
                .openingCash(
                        new ReportRowResponse("TIỀN VÀ TƯƠNG ĐƯƠNG ĐẦU KỲ", new BigDecimal("100000000"), 0, true, null))
                .closingCash(new ReportRowResponse("TIỀN VÀ TƯƠNG ĐƯƠNG CUỐI KỲ", new BigDecimal("400000000"), 0, true,
                        null))
                .build();
    }

    @Transactional(readOnly = true)
    public List<AgingResponse> getAgingReport(UUID companyId, String type, LocalDate asOfDate) {
        // Mock data cho Báo cáo tuổi nợ (AR_AGING hoặc AP_AGING)
        List<AgingResponse> result = new ArrayList<>();
        result.add(AgingResponse.builder()
                .id(UUID.randomUUID())
                .name(type.equals("AR") ? "Khách hàng A" : "Nhà cung cấp B")
                .totalAmount(new BigDecimal("15000000"))
                .current(new BigDecimal("5000000"))
                .over1To30(new BigDecimal("10000000"))
                .over31To60(BigDecimal.ZERO)
                .over61To90(BigDecimal.ZERO)
                .over90(BigDecimal.ZERO)
                .build());
        return result;
    }
}
