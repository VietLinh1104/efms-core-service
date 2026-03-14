package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Một dòng bút toán")
public class JournalLineResponse {

    @Schema(description = "ID dòng bút toán")
    private UUID id;

    @Schema(description = "UUID tài khoản")
    private UUID accountId;

    @Schema(description = "Mã tài khoản", example = "1111")
    private String accountCode;

    @Schema(description = "Tên tài khoản", example = "Tiền mặt VND")
    private String accountName;

    @Schema(description = "UUID đối tác")
    private UUID partnerId;

    @Schema(description = "Tên đối tác")
    private String partnerName;

    @Schema(description = "Số tiền Nợ")
    private BigDecimal debit;

    @Schema(description = "Số tiền Có")
    private BigDecimal credit;

    @Schema(description = "Mã tiền tệ", example = "VND")
    private String currencyCode;

    @Schema(description = "Số tiền tiền tệ gốc")
    private BigDecimal amountCurrency;

    @Schema(description = "Tỷ giá")
    private BigDecimal exchangeRate;

    @Schema(description = "Mô tả")
    private String description;

    @Schema(description = "Thời điểm tạo")
    private Instant createdAt;
}
