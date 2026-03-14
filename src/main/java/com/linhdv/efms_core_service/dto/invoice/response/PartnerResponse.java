package com.linhdv.efms_core_service.dto.invoice.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Thông tin đối tác")
public class PartnerResponse {

    @Schema(description = "ID đối tác")
    private UUID id;

    @Schema(description = "Tên", example = "Công ty TNHH ABC")
    private String name;

    @Schema(description = "Loại (customer / vendor)", example = "customer")
    private String type;

    @Schema(description = "Mã số thuế", example = "0101234567")
    private String taxCode;

    @Schema(description = "Điện thoại")
    private String phone;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Địa chỉ")
    private String address;

    @Schema(description = "ID tài khoản AR")
    private UUID arAccountId;

    @Schema(description = "Mã tài khoản AR", example = "131")
    private String arAccountCode;

    @Schema(description = "ID tài khoản AP")
    private UUID apAccountId;

    @Schema(description = "Mã tài khoản AP", example = "331")
    private String apAccountCode;

    @Schema(description = "Trạng thái hoạt động")
    private Boolean isActive;

    @Schema(description = "Thời gian tạo")
    private Instant createdAt;
}
