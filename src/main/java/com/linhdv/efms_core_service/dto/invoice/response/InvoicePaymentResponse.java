package com.linhdv.efms_core_service.dto.invoice.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Thông tin dòng phân bổ thanh toán cho hóa đơn")
public class InvoicePaymentResponse {

    @Schema(description = "ID dòng phân bổ")
    private UUID id;

    @Schema(description = "ID phiếu thanh toán gốc")
    private UUID paymentId;

    @Schema(description = "Mã hóa đơn được phân bổ", example = "INV-2025-001")
    private String invoiceNumber;

    @Schema(description = "Ngày thanh toán")
    private LocalDate paymentDate;

    @Schema(description = "Số tiền đã phân bổ")
    private BigDecimal allocatedAmount;

    @Schema(description = "Người phân bổ")
    private String createdBy;

    @Schema(description = "Thời gian phân bổ")
    private Instant createdAt;
}
