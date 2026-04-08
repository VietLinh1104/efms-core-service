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
 * Worker xử lý khi hóa đơn được DUYỆT THÀNH CÔNG.
 * Được trigger từ Event "Create Journal Entry" trong BPMN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateJournalEntryWorker {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    @JobWorker(type = "create-journal-entry")
    public void createJournalEntry(final ActivatedJob job) {
        try {
            String invoiceIdStr = (String) job.getVariablesAsMap().get("invoiceId");
            if (invoiceIdStr == null || invoiceIdStr.isBlank()) {
                log.warn("Job [{}] không chứa invoiceId", job.getKey());
                return;
            }

            UUID invoiceId = UUID.fromString(invoiceIdStr);
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);

            if (invoice != null) {
                invoice.setApprovalStatus("approved");
                
                // TODO: Gọi JournalService để sinh bút toán sổ nhật ký chung dựa trên thông tin hóa đơn (InvoiceLines).
                // Ví dụ: 
                // JournalEntry entry = journalService.createFromInvoice(invoice);
                // invoice.setJournalEntry(entry);
                
                invoiceRepository.save(invoice);
                log.info("✅ [Worker] Hóa đơn {} ĐÃ ĐƯỢC DUYỆT. (Id bút toán giả lập sinh ra... JobKey: {})", invoiceId, job.getKey());
            } else {
                log.error("❌ [Worker] Không tìm thấy dòng hóa đơn theo ID: {}", invoiceId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý CreateJournalEntryWorker: {}", e.getMessage(), e);
            throw e; // Ném lỗi để Camunda retry
        }
    }
}
