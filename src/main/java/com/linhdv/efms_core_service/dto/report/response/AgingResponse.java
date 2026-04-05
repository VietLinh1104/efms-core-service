package com.linhdv.efms_core_service.dto.report.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Báo cáo Tuổi nợ (Aging Report) cho 1 Đối tác / Hóa đơn")
public class AgingResponse {

    @Schema(description = "ID Đối tác hoặc Hóa đơn")
    private UUID id;

    @Schema(description = "Tên Đối tác")
    private String name;

    @Schema(description = "Tổng dư nợ")
    private BigDecimal totalAmount;

    @Schema(description = "Số dư chưa quá hạn (Current)")
    private BigDecimal current;

    @Schema(description = "Quá hạn từ 1 đến 30 ngày")
    private BigDecimal over1To30;

    @Schema(description = "Quá hạn từ 31 đến 60 ngày")
    private BigDecimal over31To60;

    @Schema(description = "Quá hạn từ 61 đến 90 ngày")
    private BigDecimal over61To90;

    @Schema(description = "Quá hạn trên 90 ngày")
    private BigDecimal over90;
}
