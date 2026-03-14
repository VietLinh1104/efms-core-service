package com.linhdv.efms_core_service.dto.invoice.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Payload tạo/cập nhật đối tác (Khách hàng / NCC)")
public class CreatePartnerRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Tên đối tác", example = "Công ty TNHH ABC")
    private String name;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Loại đối tác (customer / vendor)", example = "customer")
    private String type;

    @Size(max = 50)
    @Schema(description = "Mã số thuế", example = "0101234567")
    private String taxCode;

    @Size(max = 50)
    @Schema(description = "Số điện thoại", example = "0987654321")
    private String phone;

    @Size(max = 255)
    @Schema(description = "Email", example = "contact@abc.com")
    private String email;

    @Schema(description = "Địa chỉ", example = "123 Đường A, Quận B, TP.C")
    private String address;

    @NotNull
    @Schema(description = "UUID tài khoản phải thu (AR) mặc định")
    private UUID arAccountId;

    @NotNull
    @Schema(description = "UUID tài khoản phải trả (AP) mặc định")
    private UUID apAccountId;

    @Schema(description = "UUID công ty sở hữu")
    private UUID companyId;
}
