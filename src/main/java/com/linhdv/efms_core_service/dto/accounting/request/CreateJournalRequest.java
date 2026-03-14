package com.linhdv.efms_core_service.dto.accounting.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Payload tạo chứng từ kế toán")
public class CreateJournalRequest {

    @NotNull
    @Schema(description = "Ngày chứng từ", example = "2025-01-15")
    private LocalDate entryDate;

    @Schema(description = "Số tham chiếu", example = "REF-2025-001")
    private String reference;

    @Schema(description = "Mô tả chứng từ")
    private String description;

    @Schema(description = "UUID kỳ kế toán")
    private UUID periodId;

    @Schema(description = "UUID công ty")
    private UUID companyId;

    @NotEmpty
    @Valid
    @Schema(description = "Danh sách dòng bút toán (phải cân đối Nợ = Có)")
    private List<JournalLineRequest> lines;
}
