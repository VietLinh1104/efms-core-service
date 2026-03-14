package com.linhdv.efms_core_service.dto.accounting.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Một dòng bút toán trong chứng từ kế toán")
public class JournalLineRequest {

    @NotNull
    @Schema(description = "UUID tài khoản kế toán")
    private UUID accountId;

    @Schema(description = "UUID đối tác liên quan")
    private UUID partnerId;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Số tiền Nợ", example = "1000000.00")
    private BigDecimal debit;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Số tiền Có", example = "0.00")
    private BigDecimal credit;

    @Size(max = 3)
    @Schema(description = "Mã tiền tệ", example = "VND")
    private String currencyCode = "VND";

    @Schema(description = "Số tiền theo tiền tệ gốc (nếu khác VND)")
    private BigDecimal amountCurrency;

    @Schema(description = "Tỷ giá quy đổi", example = "1.000000")
    private BigDecimal exchangeRate;

    @Schema(description = "Mô tả dòng bút toán")
    private String description;
}
