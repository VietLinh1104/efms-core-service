package com.linhdv.efms_core_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bind config efms.camunda.* từ application-dev.yaml.
 *
 * Ví dụ:
 * <pre>
 * efms:
 *   camunda:
 *     tasklist-url: https://sin-1.tasklist.camunda.io/3a83fa33-...
 *     approval-threshold: 100000000
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "efms.camunda")
public class CamundaProperties {

    /** Base URL của Camunda Tasklist REST API */
    private String tasklistUrl;

    /**
     * Ngưỡng số tiền (VND) cần cấp duyệt bổ sung từ Admin.
     * Mặc định 100.000.000 VND.
     */
    private long approvalThreshold = 100_000_000L;
}
