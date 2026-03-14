package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Thông tin kỳ kế toán")
public class FiscalPeriodResponse {

    @Schema(description = "ID kỳ kế toán")
    private UUID id;

    @Schema(description = "Tên kỳ kế toán", example = "Tháng 01/2025")
    private String name;

    @Schema(description = "Ngày bắt đầu", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "Ngày kết thúc", example = "2025-01-31")
    private LocalDate endDate;

    @Schema(description = "Trạng thái kỳ (open / closed)", example = "open")
    private String status;

    @Schema(description = "Người đóng kỳ")
    private String closedBy;

    @Schema(description = "Thời điểm đóng kỳ")
    private Instant closedAt;

    @Schema(description = "Thời điểm tạo")
    private Instant createdAt;
}
