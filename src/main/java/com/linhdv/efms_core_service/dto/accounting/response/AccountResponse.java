package com.linhdv.efms_core_service.dto.accounting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Thông tin tài khoản kế toán")
public class AccountResponse {

    @Schema(description = "ID tài khoản")
    private UUID id;

    @Schema(description = "Mã tài khoản", example = "1111")
    private String code;

    @Schema(description = "Tên tài khoản", example = "Tiền mặt VND")
    private String name;

    @Schema(description = "Loại tài khoản", example = "asset")
    private String type;

    @Schema(description = "Loại số dư", example = "debit")
    private String balanceType;

    @Schema(description = "ID tài khoản cha")
    private UUID parentId;

    @Schema(description = "Tên tài khoản cha")
    private String parentName;

    @Schema(description = "Trạng thái hoạt động")
    private Boolean isActive;

    @Schema(description = "Thời điểm tạo")
    private Instant createdAt;

    @Schema(description = "Danh sách tài khoản con (dạng cây)")
    private List<AccountResponse> children;
}
