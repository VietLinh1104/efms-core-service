package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Bảng cân đối tài khoản (Trial Balance)")
public class TrialBalanceResponse {

    @Schema(description = "UUID kỳ kế toán")
    private UUID periodId;

    @Schema(description = "Tên kỳ kế toán", example = "Tháng 01/2025")
    private String periodName;

    @Schema(description = "Ngày bắt đầu")
    private LocalDate fromDate;

    @Schema(description = "Ngày kết thúc")
    private LocalDate toDate;

    @Schema(description = "Danh sách các dòng tài khoản")
    private List<TrialBalanceLineResponse> lines;
}
