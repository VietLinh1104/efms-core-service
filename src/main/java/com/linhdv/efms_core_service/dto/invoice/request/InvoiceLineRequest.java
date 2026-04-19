package com.linhdv.efms_core_service.dto.invoice.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Payload dòng hóa đơn")
public class InvoiceLineRequest {

    @Schema(description = "UUID dòng hóa đơn (null = tạo mới, có giá trị = cập nhật)")
    private UUID id;

    @NotNull
    @Schema(description = "UUID tài khoản doanh thu (AR) hoặc chi phí (AP)")
    private UUID accountId;

    @NotBlank
    @Schema(description = "Mô tả mặt hàng / dịch vụ", example = "Dịch vụ phần mềm")
    private String description;

    @NotNull
    @DecimalMin("0.01")
    @Schema(description = "Số lượng", example = "1.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Đơn giá", example = "5000000.00")
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Tỷ lệ thuế (%)", example = "10.00")
    private BigDecimal taxRate;
}
