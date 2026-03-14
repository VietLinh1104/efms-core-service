package com.linhdv.efms_core_service.dto.invoice.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Thông tin phiếu thu/chi")
public class PaymentResponse {

    @Schema(description = "ID thanh toán")
    private UUID id;

    @Schema(description = "Loại thanh toán (in / out)", example = "in")
    private String paymentType;

    @Schema(description = "ID Đối tác")
    private UUID partnerId;

    @Schema(description = "Tên Đối tác", example = "Công ty TNHH XYZ")
    private String partnerName;

    @Schema(description = "Ngày thanh toán")
    private LocalDate paymentDate;

    @Schema(description = "Loại tiền", example = "VND")
    private String currencyCode;

    @Schema(description = "Tổng số tiền thanh toán")
    private BigDecimal amount;

    @Schema(description = "Phương thức (bank, cash...)", example = "bank")
    private String paymentMethod;

    @Schema(description = "Diễn giải / tham chiếu", example = "Thu tiền hóa đơn 001")
    private String reference;

    @Schema(description = "ID bút toán (nếu đã post)")
    private UUID journalEntryId;

    @Schema(description = "Người nhận/chi")
    private String createdBy;

    @Schema(description = "Thời gian tạo")
    private Instant createdAt;

    @Schema(description = "Chi tiết các hóa đơn đã được phân bổ (Nếu gọi detail)")
    private List<InvoicePaymentResponse> allocations;
}
