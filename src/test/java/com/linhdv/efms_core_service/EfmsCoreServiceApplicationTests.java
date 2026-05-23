package com.linhdv.efms_core_service;

import com.linhdv.efms_core_service.entity.AuditLog;
import com.linhdv.efms_core_service.repository.AuditLogRepository;
import com.linhdv.efms_core_service.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EfmsCoreServiceApplicationTests {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testAuditLogSecurityContextPropagation() throws Exception {
        // Given
        UUID mockUserId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test@example.com", null, List.of()
        );
        auth.setDetails(mockUserId.toString());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID recordId = UUID.randomUUID();

        // When
        auditService.log("test_table", recordId, "INSERT", null, null);

        // Then (wait for async execution to complete)
        List<AuditLog> logs = null;
        for (int i = 0; i < 20; i++) {
            logs = auditLogRepository.findByTableNameAndRecordIdOrderByChangedAtAsc("test_table", recordId);
            if (!logs.isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }

        assertNotNull(logs);
        assertEquals(1, logs.size());
        AuditLog savedLog = logs.get(0);
        assertNotNull(savedLog.getChangedBy());
        assertEquals(mockUserId, savedLog.getChangedBy());

        // Cleanup
        auditLogRepository.deleteAll(logs);
    }
}
