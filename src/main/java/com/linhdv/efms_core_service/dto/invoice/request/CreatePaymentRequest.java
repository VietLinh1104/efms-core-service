package com.linhdv.efms_core_service.dto.invoice.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Schema(description = "Payload tạo/cập nhật phiếu thu/chi")
public class CreatePaymentRequest {

    @NotBlank
    @Size(max = 10)
    @Schema(description = "Loại giao dịch (in / out)", example = "in")
    private String paymentType;

    @NotNull
    @Schema(description = "UUID đối tác")
    private UUID partnerId;

    @NotNull
    @Schema(description = "Ngày giao dịch", example = "2025-01-15")
    private LocalDate paymentDate;

    @Size(max = 3)
    @Schema(description = "Loại tiền tệ", example = "VND")
    private String currencyCode = "VND";

    @Schema(description = "Tỷ giá quy đổi (mặc định 1)", example = "1.000000")
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @NotNull
    @DecimalMin("0.01")
    @Schema(description = "Số tiền thanh toán", example = "1000000.00")
    private BigDecimal amount;

    @Size(max = 50)
    @Schema(description = "Phương thức thanh toán (cash, bank, card...)", example = "bank")
    private String paymentMethod;

    @Schema(description = "UUID tài khoản ngân hàng (nếu method=bank)")
    private UUID bankAccountId;

    @Size(max = 255)
    @Schema(description = "Số tham chiếu / Lý do", example = "Thu tiền hóa đơn INV-001")
    private String reference;

    @Schema(description = "UUID công ty sở hữu")
    private UUID companyId;
}
