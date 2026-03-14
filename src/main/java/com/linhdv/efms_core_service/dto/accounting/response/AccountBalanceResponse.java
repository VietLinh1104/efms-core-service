package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Số dư tài khoản theo kỳ hoặc ngày")
public class AccountBalanceResponse {

    @Schema(description = "Mã tài khoản", example = "1111")
    private String accountCode;

    @Schema(description = "Tên tài khoản", example = "Tiền mặt VND")
    private String accountName;

    @Schema(description = "Tổng phát sinh Nợ trong kỳ")
    private BigDecimal totalDebit;

    @Schema(description = "Tổng phát sinh Có trong kỳ")
    private BigDecimal totalCredit;

    @Schema(description = "Số dư đầu kỳ")
    private BigDecimal openingBalance;

    @Schema(description = "Số dư cuối kỳ")
    private BigDecimal closingBalance;
}
