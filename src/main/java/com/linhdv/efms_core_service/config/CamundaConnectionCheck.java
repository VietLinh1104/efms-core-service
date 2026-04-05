package com.linhdv.efms_core_service.config;

import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Tự động kiểm tra kết nối tới Camunda 8 khi ứng dụng khởi động (chỉ ở profile dev).
 *
 * SDK 8.8.x dùng CamundaClient (không phải ZeebeClient nữa).
 * Nếu kết nối thành công sẽ in danh sách Brokers ra log.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class CamundaConnectionCheck implements CommandLineRunner {

    private final CamundaClient camundaClient;

    @Override
    public void run(String... args) {
        log.info("=== Đang kiểm tra kết nối tới Camunda 8 SaaS ===");
        try {
            var topology = camundaClient.newTopologyRequest().send().join();
            log.info("✅ Kết nối Camunda THÀNH CÔNG!");
            log.info("Brokers available: {}", topology.getBrokers().size());
            topology.getBrokers().forEach(broker ->
                log.info(" - Broker: {}:{}", broker.getHost(), broker.getPort())
            );
        } catch (Exception e) {
            log.error("❌ Kết nối Camunda THẤT BẠI!");
            log.error("Chi tiết lỗi: {}", e.getMessage());
            log.error("Gợi ý: Kiểm tra lại client-id, client-secret trong application-dev.yaml");
        }
    }
}
