package com.linhdv.efms_core_service.service.accounting;

import com.linhdv.efms_core_service.dto.accounting.response.JournalEntryResponse;
import com.linhdv.efms_core_service.dto.accounting.response.JournalLineResponse;
import com.linhdv.efms_core_service.mapper.accounting.JournalMapper;
import com.linhdv.efms_core_service.repository.accounting.JournalEntryRepository;
import com.linhdv.efms_core_service.repository.accounting.JournalLineRepository;
import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JournalService — Quản lý bút toán kế toán.
 * Bút toán chỉ được tạo tự động bởi hệ thống (khi Invoice được duyệt hoặc Payment được post).
 * Không hỗ trợ tạo/sửa/xóa bút toán thủ công.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalMapper journalMapper;

    // ── Tạo bút toán tự động từ Invoice được phê duyệt ───────────────────────

    /**
     * Sinh bút toán kép tự động khi AP Bill được phê duyệt.
     *
     * <p>Quy tắc double-entry cho AP Bill:
     * <ul>
     *   <li>Mỗi dòng hóa đơn → Nợ (Debit) TK chi phí = amount + taxAmount</li>
     *   <li>Một dòng tổng → Có (Credit) TK phải trả AP (partner.apAccount) = totalAmount</li>
     * </ul>
     *
     * @param invoice   Invoice entity đã được approve (status=open, approvalStatus=approved)
     * @param lines     Danh sách các dòng chi tiết của invoice (đã load đầy đủ)
     * @return          JournalEntry đã được persist
     */
    @Transactional
    public JournalEntry createFromInvoice(Invoice invoice, List<InvoiceLine> lines) {
        // 1. Tạo đầu Journal Entry
        JournalEntry je = new JournalEntry();
        je.setCompanyId(invoice.getCompanyId());
        je.setEntryDate(invoice.getInvoiceDate());
        je.setReference(invoice.getInvoiceNumber());
        je.setDescription("AP Bill được duyệt: " + invoice.getInvoiceNumber()
                + " — " + invoice.getPartner().getName());
        je.setStatus("posted");
        je.setSource("invoice");
        je.setSourceRefId(invoice.getId());
        je.setCreatedBy(invoice.getCreatedBy());
        je.setCreatedAt(Instant.now());
        JournalEntry savedJe = journalEntryRepository.save(je);

        String currency = invoice.getCurrencyCode() != null ? invoice.getCurrencyCode() : "VND";
        BigDecimal exchangeRate = invoice.getExchangeRate() != null ? invoice.getExchangeRate() : BigDecimal.ONE;

        // 2. Tạo dòng Nợ — mỗi invoice line → 1 journal line Nợ TK chi phí
        for (InvoiceLine line : lines) {
            BigDecimal lineTotal = line.getAmount().add(line.getTaxAmount());

            JournalLine debitLine = new JournalLine();
            debitLine.setJournalEntry(savedJe);
            debitLine.setAccount(line.getAccount());
            debitLine.setPartner(invoice.getPartner());
            debitLine.setDebit(lineTotal);
            debitLine.setCredit(BigDecimal.ZERO);
            debitLine.setCurrencyCode(currency);
            debitLine.setAmountCurrency(lineTotal);
            debitLine.setExchangeRate(exchangeRate);
            debitLine.setDescription(line.getDescription());
            debitLine.setCreatedAt(Instant.now());
            journalLineRepository.save(debitLine);
        }

        // 3. Tạo dòng Có — một dòng tổng → Có TK phải trả AP (apAccount của Partner)
        Account apAccount = invoice.getPartner().getApAccount();
        if (apAccount == null) {
            throw new IllegalStateException(
                    "Đối tác [" + invoice.getPartner().getId() + "] chưa cấu hình TK phải trả (AP Account).");
        }

        JournalLine creditLine = new JournalLine();
        creditLine.setJournalEntry(savedJe);
        creditLine.setAccount(apAccount);
        creditLine.setPartner(invoice.getPartner());
        creditLine.setDebit(BigDecimal.ZERO);
        creditLine.setCredit(invoice.getTotalAmount());
        creditLine.setCurrencyCode(currency);
        creditLine.setAmountCurrency(invoice.getTotalAmount());
        creditLine.setExchangeRate(exchangeRate);
        creditLine.setDescription("Công nợ phải trả: " + invoice.getPartner().getName());
        creditLine.setCreatedAt(Instant.now());
        journalLineRepository.save(creditLine);

        log.info("✅ JournalEntry [{}] đã được tạo tự động từ AP Bill [{}]", savedJe.getId(), invoice.getId());
        return savedJe;
    }


    @Transactional(readOnly = true)
    public PagedResponse<JournalEntryResponse> list(UUID companyId, String status,
            LocalDate fromDate, LocalDate toDate,
            int page, int size) {
        Page<JournalEntry> result = journalEntryRepository.search(
                companyId, status, fromDate, toDate, PageRequest.of(page, size));

        List<JournalEntryResponse> content = result.getContent()
                .stream()
                .map(journalMapper::toResponse)
                .toList();

        return PagedResponse.of(content, page, size, result.getTotalElements());
    }

    // ── Chi tiết (kèm dòng bút toán) ──────────────────────────────────────

    @Transactional(readOnly = true)
    public JournalEntryResponse getDetail(UUID id) {
        JournalEntry je = findOrThrow(id);
        List<JournalLineResponse> lines = journalLineRepository
                .findByJournalEntryIdOrderByCreatedAt(id)
                .stream()
                .map(journalMapper::toLineResponse)
                .toList();

        JournalEntryResponse resp = journalMapper.toResponse(je);
        resp.setLines(lines);
        return resp;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JournalEntry findOrThrow(UUID id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bút toán không tồn tại: " + id));
    }
}
