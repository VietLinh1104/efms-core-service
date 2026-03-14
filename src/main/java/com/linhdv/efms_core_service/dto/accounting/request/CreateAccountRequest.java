package com.linhdv.efms_core_service.dto.accounting.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Payload tạo / cập nhật tài khoản kế toán")
public class CreateAccountRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Mã tài khoản (VD: 1111)", example = "1111")
    private String code;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Tên tài khoản", example = "Tiền mặt VND")
    private String name;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Loại tài khoản (asset, liability, equity, revenue, expense)", example = "asset")
    private String type;

    @NotBlank
    @Size(max = 10)
    @Schema(description = "Loại số dư (debit / credit)", example = "debit")
    private String balanceType;

    @Schema(description = "UUID tài khoản cha (nếu là tài khoản con)")
    private UUID parentId;

    @Schema(description = "UUID công ty sở hữu tài khoản")
    private UUID companyId;
}
