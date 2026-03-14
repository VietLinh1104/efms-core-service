package com.linhdv.efms_core_service.dto.invoice.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Chi tiết một dòng hóa đơn")
public class InvoiceLineResponse {

    @Schema(description = "ID dòng hóa đơn")
    private UUID id;

    @Schema(description = "ID tài khoản")
    private UUID accountId;

    @Schema(description = "Mã tài khoản", example = "5111")
    private String accountCode;

    @Schema(description = "Tên tài khoản", example = "Doanh thu phần mềm")
    private String accountName;

    @Schema(description = "Mô tả mặt hàng")
    private String description;

    @Schema(description = "Số lượng")
    private BigDecimal quantity;

    @Schema(description = "Đơn giá")
    private BigDecimal unitPrice;

    @Schema(description = "Tỷ lệ thuế (%)")
    private BigDecimal taxRate;

    @Schema(description = "Tiền thuế")
    private BigDecimal taxAmount;

    @Schema(description = "Thành tiền (trước thuế)")
    private BigDecimal amount;
}
