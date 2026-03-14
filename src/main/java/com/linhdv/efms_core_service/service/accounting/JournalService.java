package com.linhdv.efms_core_service.service.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateJournalRequest;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    // ── Tạo mới (draft) ───────────────────────────────────────────────────

    @Transactional
    public JournalEntryResponse create(CreateJournalRequest req) {
        validateBalance(req);

        Company company = new Company();
        company.setId(req.getCompanyId());

        JournalEntry je = new JournalEntry();
        je.setCompany(company);
        je.setEntryDate(req.getEntryDate());
        je.setReference(req.getReference());
        je.setDescription(req.getDescription());
        je.setStatus("draft");
        je.setSource("manual");
        je.setCreatedAt(Instant.now());

        if (req.getPeriodId() != null) {
            FiscalPeriod period = new FiscalPeriod();
            period.setId(req.getPeriodId());
            je.setPeriod(period);
        }

        JournalEntry saved = journalEntryRepository.save(je);

        // Tạo các dòng bút toán
        req.getLines().forEach(lineReq -> {
            JournalLine line = new JournalLine();
            line.setJournalEntry(saved);
            Account acc = new Account();
            acc.setId(lineReq.getAccountId());
            line.setAccount(acc);
            line.setDebit(lineReq.getDebit());
            line.setCredit(lineReq.getCredit());
            line.setCurrencyCode(lineReq.getCurrencyCode() != null ? lineReq.getCurrencyCode() : "VND");
            line.setAmountCurrency(lineReq.getAmountCurrency());
            line.setExchangeRate(lineReq.getExchangeRate() != null ? lineReq.getExchangeRate() : BigDecimal.ONE);
            line.setDescription(lineReq.getDescription());
            line.setCreatedAt(Instant.now());
            journalLineRepository.save(line);
        });

        return toResponse(saved);
    }

    // ── Post chứng từ ─────────────────────────────────────────────────────

    @Transactional
    public JournalEntryResponse post(UUID id) {
        JournalEntry je = findOrThrow(id);
        if (!"draft".equals(je.getStatus())) {
            throw new IllegalStateException("Chỉ có thể post chứng từ ở trạng thái draft");
        }
        je.setStatus("posted");
        je.setPostedAt(Instant.now());
        return toResponse(journalEntryRepository.save(je));
    }

    // ── Huỷ chứng từ ──────────────────────────────────────────────────────

    @Transactional
    public JournalEntryResponse cancel(UUID id) {
        JournalEntry je = findOrThrow(id);
        if ("cancelled".equals(je.getStatus())) {
            throw new IllegalStateException("Chứng từ đã bị huỷ");
        }
        je.setStatus("cancelled");
        return toResponse(journalEntryRepository.save(je));
    }

    // ── Xoá (chỉ draft) ───────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        JournalEntry je = findOrThrow(id);
        if (!"draft".equals(je.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xoá chứng từ ở trạng thái draft");
        }
        journalEntryRepository.delete(je);
    }

    // ── Validate ──────────────────────────────────────────────────────────

    private void validateBalance(CreateJournalRequest req) {
        BigDecimal totalDebit = req.getLines().stream().map(l -> l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = req.getLines().stream()
                .map(l -> l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    "Chứng từ không cân đối: Tổng Nợ (" + totalDebit + ") ≠ Tổng Có (" + totalCredit + ")");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JournalEntry findOrThrow(UUID id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chứng từ không tồn tại: " + id));
    }

    private JournalEntryResponse toResponse(JournalEntry je) {
        return JournalEntryResponse.builder()
                .id(je.getId())
                .entryDate(je.getEntryDate())
                .reference(je.getReference())
                .description(je.getDescription())
                .status(je.getStatus())
                .source(je.getSource())
                .periodId(je.getPeriod() != null ? je.getPeriod().getId() : null)
                .periodName(je.getPeriod() != null ? je.getPeriod().getName() : null)
                .createdBy(je.getCreatedBy() != null ? je.getCreatedBy().getName() : null)
                .postedBy(je.getPostedBy() != null ? je.getPostedBy().getName() : null)
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
