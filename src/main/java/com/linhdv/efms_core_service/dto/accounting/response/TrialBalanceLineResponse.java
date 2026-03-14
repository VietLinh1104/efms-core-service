package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Một dòng trong Bảng cân đối tài khoản (Trial Balance)")
public class TrialBalanceLineResponse {

    @Schema(description = "Mã tài khoản", example = "1111")
    private String accountCode;

    @Schema(description = "Tên tài khoản", example = "Tiền mặt VND")
    private String accountName;

    @Schema(description = "Số dư đầu kỳ Nợ")
    private BigDecimal openingDebit;

    @Schema(description = "Số dư đầu kỳ Có")
    private BigDecimal openingCredit;

    @Schema(description = "Phát sinh Nợ trong kỳ")
    private BigDecimal periodDebit;

    @Schema(description = "Phát sinh Có trong kỳ")
    private BigDecimal periodCredit;

    @Schema(description = "Số dư cuối kỳ Nợ")
    private BigDecimal closingDebit;

    @Schema(description = "Số dư cuối kỳ Có")
    private BigDecimal closingCredit;
}
