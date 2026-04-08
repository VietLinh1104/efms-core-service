package com.linhdv.efms_core_service.service.camunda;

import com.linhdv.efms_core_service.config.TasklistTokenProvider;
import com.linhdv.efms_core_service.config.ZeebeTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * REST Client tích hợp Camunda 8 SaaS.
 *
 * - findTaskIdByProcessInstanceKey: Dùng Tasklist REST API v1 (search tasks)
 * - completeTask: Dùng Zeebe REST API v2 — bắt buộc với Zeebe User Task (<zeebe:userTask />)
 *
 * NOTE: Tasklist V1 API không support complete cho Zeebe User Task.
 * Phải dùng POST /v2/user-tasks/{key}/completion với Zeebe REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TasklistApiClient {

    private final WebClient webClient;
    private final TasklistTokenProvider tasklistTokenProvider;
    private final ZeebeTokenProvider zeebeTokenProvider;

    @Value("${efms.camunda.tasklist-url}")
    private String tasklistUrl;

    @Value("${efms.camunda.zeebe-rest-url}")
    private String zeebeRestUrl;

    /**
     * Tìm ID (userTaskKey) của Zeebe User Task đang ở trạng thái CREATED
     * thuộc về một processInstanceKey cụ thể, qua Tasklist REST API v1.
     */
    public String findTaskIdByProcessInstanceKey(String processInstanceKey) {
        String token = tasklistTokenProvider.getToken();

        Map<String, Object> searchBody = Map.of(
                "processInstanceKey", processInstanceKey,
                "state", "CREATED"
        );

        List<Map<String, Object>> response = webClient.post()
                .uri(tasklistUrl + "/v1/tasks/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(searchBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();

        if (response != null && !response.isEmpty()) {
            log.info("🔍 Tìm thấy {} task đang chờ duyệt cho processInstanceKey: {}", response.size(), processInstanceKey);
            return (String) response.get(0).get("id");
        }
        log.warn("⚠️ Không tìm thấy task nào ở trạng thái CREATED cho processInstanceKey: {}", processInstanceKey);
        return null;
    }

    /**
     * Complete một Zeebe User Task bằng Zeebe REST API v2.
     *
     * Endpoint: POST /v2/user-tasks/{userTaskKey}/completion
     * Variables: flat JSON map (không phải list như Tasklist v1)
     * Token audience: zeebe.camunda.io
     *
     * @param taskId  userTaskKey (numeric string) lấy từ Tasklist search
     * @param approved kết quả phê duyệt
     * @param comment  ghi chú
     */
    public void completeTask(String taskId, boolean approved, String comment) {
        String token = zeebeTokenProvider.getToken();

        // Zeebe REST API v2: variables là flat JSON object, không phải List<{name,value}>
        Map<String, Object> variables = Map.of(
                "approved", approved,
                "comment", comment == null ? "" : comment
        );

        Map<String, Object> completeBody = Map.of("variables", variables);

        log.info("📤 Gửi complete task [{}] via Zeebe REST API — approved={}, comment={}", taskId, approved, comment);

        try {
            webClient.post()
                    .uri(zeebeRestUrl + "/v2/user-tasks/" + taskId + "/completion")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(completeBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            resp -> resp.bodyToMono(String.class).map(body -> {
                                log.error("❌ Zeebe complete task thất bại: HTTP {} — Response: {}", resp.statusCode(), body);
                                return new RuntimeException("Zeebe complete task failed [" + resp.statusCode() + "]: " + body);
                            })
                    )
                    .bodyToMono(Void.class)
                    .block();

            log.info("✅ Complete thành công Zeebe User Task [{}] (approved={})", taskId, approved);

        } catch (Exception e) {
            log.error("Lỗi khi gọi Zeebe Complete Task: {}", e.getMessage(), e);
            throw new RuntimeException("Zeebe API call failed", e);
        }
    }
}
