package com.linhdv.efms_core_service.dto.accounting.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Schema(description = "Payload tạo kỳ kế toán")
public class CreateFiscalPeriodRequest {

    @NotBlank
    @Schema(description = "Tên kỳ kế toán", example = "Tháng 01/2025")
    private String name;

    @NotNull
    @Schema(description = "Ngày bắt đầu kỳ", example = "2025-01-01")
    private LocalDate startDate;

    @NotNull
    @Schema(description = "Ngày kết thúc kỳ", example = "2025-01-31")
    private LocalDate endDate;

    @Schema(description = "UUID công ty")
    private UUID companyId;
}
