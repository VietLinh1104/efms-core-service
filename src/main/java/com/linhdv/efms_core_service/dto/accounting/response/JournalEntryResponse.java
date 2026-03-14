package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Thông tin chứng từ kế toán")
public class JournalEntryResponse {

    @Schema(description = "ID chứng từ")
    private UUID id;

    @Schema(description = "Ngày chứng từ", example = "2025-01-15")
    private LocalDate entryDate;

    @Schema(description = "Số tham chiếu", example = "REF-2025-001")
    private String reference;

    @Schema(description = "Mô tả chứng từ")
    private String description;

    @Schema(description = "Trạng thái (draft / posted / cancelled)", example = "draft")
    private String status;

    @Schema(description = "Nguồn tạo (manual, invoice, payment, ...)", example = "manual")
    private String source;

    @Schema(description = "ID kỳ kế toán")
    private UUID periodId;

    @Schema(description = "Tên kỳ kế toán", example = "Tháng 01/2025")
    private String periodName;

    @Schema(description = "Người tạo")
    private String createdBy;

    @Schema(description = "Người post")
    private String postedBy;

    @Schema(description = "Thời điểm post")
    private Instant postedAt;

    @Schema(description = "Thời điểm tạo")
    private Instant createdAt;

    @Schema(description = "Danh sách dòng bút toán (chỉ có trong API detail)")
    private List<JournalLineResponse> lines;
}
