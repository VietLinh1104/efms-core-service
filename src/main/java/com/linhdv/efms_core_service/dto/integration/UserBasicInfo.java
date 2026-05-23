package com.linhdv.efms_core_service.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfo {
    private UUID id;
    private String fullName;
    private String email;
    private String avatar;
}
