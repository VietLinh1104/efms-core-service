package com.linhdv.efms_core_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Kích hoạt bảo mật ở mức Method (như @PreAuthorize)
 * Chỉ áp dụng cho các môi trường KHÁC 'dev' (như prod, staging)
 */
@Configuration
@Profile("!dev")
@EnableMethodSecurity
public class MethodSecurityConfig {
}
