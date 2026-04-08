package com.linhdv.efms_core_service.service.worker;

import com.linhdv.efms_core_service.entity.Invoice;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Worker xử lý khi hóa đơn bị TỪ CHỐI.
 * Được trigger từ Event "Notify Rejection" trong BPMN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRejectionWorker {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    @JobWorker(type = "notify-rejection")
    public void notifyRejection(final ActivatedJob job) {
        try {
            String invoiceIdStr = (String) job.getVariablesAsMap().get("invoiceId");
            if (invoiceIdStr == null || invoiceIdStr.isBlank()) {
                log.warn("Job [{}] không chứa invoiceId", job.getKey());
                return;
            }

            UUID invoiceId = UUID.fromString(invoiceIdStr);
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);

            if (invoice != null) {
                invoice.setApprovalStatus("rejected");
                invoiceRepository.save(invoice);
                log.info("✅ [Worker] Đã cập nhật trạng thái hóa đơn {} thành REJECTED", invoiceId);
            } else {
                log.error("❌ [Worker] Không tìm thấy hóa đơn ID: {}", invoiceId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý NotifyRejectionWorker: {}", e.getMessage(), e);
            throw e; // Ném ra lỗi để Camunda 8 thu hồi job và retry
        }
    }
}
