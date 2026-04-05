package com.linhdv.efms_core_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cấu hình WebClient để gọi Camunda Tasklist REST API.
 *
 * Authentication sử dụng OAuth2 client_credentials — token được lấy
 * và inject vào mỗi request khi gọi Tasklist API.
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient "thô" — không gắn base URL, dùng để lấy OAuth2 token
     * và cho các HTTP call chung.
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
