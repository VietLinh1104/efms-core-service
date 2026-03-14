package com.linhdv.efms_core_service.dto.invoice.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Payload tạo/cập nhật hóa đơn")
public class CreateInvoiceRequest {

    @NotBlank
    @Size(max = 5)
    @Schema(description = "Loại hóa đơn (AR: thu, AP: chi)", example = "AR")
    private String invoiceType;

    @NotNull
    @Schema(description = "UUID đối tác")
    private UUID partnerId;

    @Size(max = 100)
    @Schema(description = "Số hóa đơn (nếu có)", example = "INV-2025-001")
    private String invoiceNumber;

    @NotNull
    @Schema(description = "Ngày phát hành", example = "2025-01-01")
    private LocalDate invoiceDate;

    @Schema(description = "Ngày đến hạn", example = "2025-01-31")
    private LocalDate dueDate;

    @Size(max = 3)
    @Schema(description = "Loại tiền tệ", example = "VND")
    private String currencyCode = "VND";

    @Schema(description = "Tỷ giá quy đổi (mặc định 1)", example = "1.000000")
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Schema(description = "UUID công ty sở hữu")
    private UUID companyId;

    @NotEmpty
    @Valid
    @Schema(description = "Danh sách dòng hóa đơn")
    private List<InvoiceLineRequest> lines;
}
