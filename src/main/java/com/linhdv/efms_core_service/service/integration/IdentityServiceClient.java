package com.linhdv.efms_core_service.service.integration;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import com.linhdv.efms_core_service.dto.integration.UserBasicInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IdentityServiceClient {

    private final RestClient restClient;

    @Value("${efms.integration.identity-url}")
    private String identityUrl;

    public IdentityServiceClient() {
        this.restClient = RestClient.create();
    }

    public Map<UUID, UserBasicInfo> getBatchUsers(Set<UUID> userIds, UUID companyId) {
        if (userIds == null || userIds.isEmpty() || companyId == null) {
            return Collections.emptyMap();
        }

        try {
            log.debug("Batch fetching {} users from Identity Service for company {}", userIds.size(), companyId);

            ApiResponse<List<UserBasicInfo>> response = restClient.post()
                    .uri(identityUrl + "/internal/users/batch")
                    .header("X-Company-Id", companyId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<UserBasicInfo>>>() {
                    });

            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(UserBasicInfo::getId, user -> user));
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Identity Service lấy thông tin users: {}", e.getMessage(), identityUrl);
        }

        return Collections.emptyMap();
    }
}
