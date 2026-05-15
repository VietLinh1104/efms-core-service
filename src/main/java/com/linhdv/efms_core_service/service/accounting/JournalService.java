package com.linhdv.efms_core_service.service.accounting;

import com.linhdv.efms_core_service.dto.accounting.response.JournalEntryResponse;
import com.linhdv.efms_core_service.dto.accounting.response.JournalLineResponse;
import com.linhdv.efms_core_service.repository.accounting.JournalEntryRepository;
import com.linhdv.efms_core_service.repository.accounting.JournalLineRepository;
import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JournalService — Read-only (Đồ án scope).
 * Bút toán chỉ được tạo tự động bởi hệ thống (qua Camunda Worker hoặc PaymentService).
 * Không hỗ trợ tạo/sửa/xóa bút toán thủ công.
 */
@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;

    // ── Danh sách (phân trang) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<JournalEntryResponse> list(UUID companyId, String status,
            LocalDate fromDate, LocalDate toDate,
            int page, int size) {
        Page<JournalEntry> result = journalEntryRepository.search(
                companyId, status, fromDate, toDate, PageRequest.of(page, size));

        List<JournalEntryResponse> content = result.getContent()
                .stream()
                .map(this::toResponse)
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
                .map(this::toLineResponse)
                .toList();

        JournalEntryResponse resp = toResponse(je);
        resp.setLines(lines);
        return resp;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JournalEntry findOrThrow(UUID id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bút toán không tồn tại: " + id));
    }

    private JournalEntryResponse toResponse(JournalEntry je) {
        return JournalEntryResponse.builder()
                .id(je.getId())
                .entryDate(je.getEntryDate())
                .reference(je.getReference())
                .description(je.getDescription())
                .status(je.getStatus())
                .source(je.getSource())
                .periodId(je.getPeriodId())
                .createdBy(je.getCreatedBy() != null ? je.getCreatedBy() : null)
                .postedBy(je.getPostedBy() != null ? je.getPostedBy() : null)
                .postedAt(je.getPostedAt())
                .createdAt(je.getCreatedAt())
                .build();
    }

    private JournalLineResponse toLineResponse(JournalLine line) {
        return JournalLineResponse.builder()
                .id(line.getId())
                .accountId(line.getAccount().getId())
                .accountCode(line.getAccount().getCode())
                .accountName(line.getAccount().getName())
                .partnerId(line.getPartner() != null ? line.getPartner().getId() : null)
                .partnerName(line.getPartner() != null ? line.getPartner().getName() : null)
                .debit(line.getDebit())
                .credit(line.getCredit())
                .currencyCode(line.getCurrencyCode())
                .amountCurrency(line.getAmountCurrency())
                .exchangeRate(line.getExchangeRate())
                .description(line.getDescription())
                .createdAt(line.getCreatedAt())
                .build();
    }
}
