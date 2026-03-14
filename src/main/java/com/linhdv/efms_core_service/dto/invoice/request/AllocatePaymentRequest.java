package com.linhdv.efms_core_service.dto.invoice.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Payload phân bổ thanh toán vào một hoá đơn")
public class AllocatePaymentRequest {

    @NotNull
    @Schema(description = "UUID của chứng từ Hóa đơn cần trừ nợ")
    private UUID invoiceId;

    @NotNull
    @DecimalMin("0.01")
    @Schema(description = "Số tiền thanh toán sẽ gán vào hóa đơn này", example = "500000.00")
    private BigDecimal amount;
}
