package com.linhdv.efms_core_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Lấy OAuth2 Token để gọi Camunda Zeebe REST API (v2).
 * Audience khác với Tasklist: "zeebe.camunda.io"
 */
@Slf4j
@Component
public class ZeebeTokenProvider {

    private final WebClient webClient;

    @Value("${camunda.client.auth.client-id}")
    private String clientId;

    @Value("${camunda.client.auth.client-secret}")
    private String clientSecret;

    private static final String OAUTH_URL = "https://login.cloud.camunda.io/oauth/token";

    public ZeebeTokenProvider(WebClient webClient) {
        this.webClient = webClient;
    }

    @SuppressWarnings("unchecked")
    public String getToken() {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("audience", "zeebe.camunda.io");   // ← khác Tasklist
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            Map<String, Object> response = webClient.post()
                    .uri(OAUTH_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
            throw new RuntimeException("Không lấy được access_token từ Camunda Auth Server (Zeebe)");
        } catch (Exception e) {
            log.error("Lỗi khi xin OAuth token cho Zeebe REST API: {}", e.getMessage());
            throw new RuntimeException("Zeebe Auth Error", e);
        }
    }
}
