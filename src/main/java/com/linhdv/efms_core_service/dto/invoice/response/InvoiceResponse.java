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
@Schema(description = "Thông tin hóa đơn")
public class InvoiceResponse {

    @Schema(description = "ID hóa đơn")
    private UUID id;

    @Schema(description = "Phân loại hóa đơn (AR/AP)", example = "AR")
    private String invoiceType;

    @Schema(description = "Số hóa đơn", example = "INV-2025-001")
    private String invoiceNumber;

    @Schema(description = "ID Đối tác")
    private UUID partnerId;

    @Schema(description = "Tên Đối tác", example = "Công ty TNHH ABC")
    private String partnerName;

    @Schema(description = "Ngày hóa đơn")
    private LocalDate invoiceDate;

    @Schema(description = "Ngày đến hạn")
    private LocalDate dueDate;

    @Schema(description = "Mã tiền tệ")
    private String currencyCode;

    @Schema(description = "Tỷ giá")
    private BigDecimal exchangeRate;

    @Schema(description = "Tổng tiền trước thuế")
    private BigDecimal subtotal;

    @Schema(description = "Tổng tiền thuế")
    private BigDecimal taxAmount;

    @Schema(description = "Tổng giá trị hóa đơn (sau thuế)")
    private BigDecimal totalAmount;

    @Schema(description = "Số tiền đã thanh toán")
    private BigDecimal paidAmount;

    @Schema(description = "Trạng thái (draft, open, in_payment, paid, cancelled)", example = "draft")
    private String status;

    @Schema(description = "Người lập")
    private UUID createdBy;

    @Schema(description = "Thời điểm tạo")
    private Instant createdAt;

    @Schema(description = "Trạng thái duyệt (pending, approved, rejected)")
    private String approvalStatus;

    @Schema(description = "ID xử lý BPMN Camunda")
    private String camundaProcessId;

    @Schema(description = "ID Bút toán liên kết (nếu đã confirm)")
    private UUID journalEntryId;

    @Schema(description = "Chi tiết các dòng hóa đơn (chỉ có khi gọi detail)")
    private List<InvoiceLineResponse> lines;
}
