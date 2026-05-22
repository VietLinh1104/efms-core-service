package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceRequest;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceLineRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceLineResponse;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.repository.invoice.InvoiceLineRepository;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import com.linhdv.efms_core_service.repository.invoice.PartnerRepository;
import com.linhdv.efms_core_service.service.accounting.JournalService;
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
 * InvoiceService — quản lý vòng đời hóa đơn AP/AR.
 * Không còn tích hợp Camunda. Trạng thái phê duyệt được lưu trực tiếp vào DB.
 *
 * Luồng trạng thái AP Bill:
 *   draft → (confirm) → open [approval_status=pending]
 *         → (approve) → open [approval_status=approved]  → sinh JournalEntry
 *         → (reject)  → open [approval_status=rejected]
 *         → (cancel)  → cancelled
 *
 * Luồng AR Invoice:
 *   draft → (confirm) → open (không cần phê duyệt)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final PartnerRepository partnerRepository;
    private final JournalService journalService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> search(UUID companyId, String type, String status, UUID partnerId, int page, int size) {
        Page<Invoice> data = invoiceRepository.search(companyId, type, status, partnerId, PageRequest.of(page, size));
        List<InvoiceResponse> content = data.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getDetail(UUID id) {
        Invoice invoice = findOrThrow(id);
        List<InvoiceLineResponse> lines = invoiceLineRepository.findByInvoiceIdOrderByIdAsc(id)
                .stream().map(this::toLineResponse).toList();

        InvoiceResponse resp = toResponse(invoice);
        resp.setLines(lines);
        return resp;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest req) {
        Partner partner = new Partner(); partner.setId(req.getPartnerId());

        Invoice invoice = new Invoice();
        invoice.setCompanyId(req.getCompanyId());
        invoice.setPartner(partner);
        invoice.setInvoiceType(req.getInvoiceType());
        invoice.setInvoiceNumber(req.getInvoiceNumber());
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setDueDate(req.getDueDate());
        invoice.setCurrencyCode(req.getCurrencyCode() != null ? req.getCurrencyCode() : "VND");
        invoice.setExchangeRate(req.getExchangeRate() != null ? req.getExchangeRate() : BigDecimal.ONE);
        invoice.setStatus("draft");
        invoice.setCreatedAt(Instant.now());
        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);

        Invoice saved = invoiceRepository.save(invoice);
        BigDecimal[] totals = saveLines(saved, req.getLines(), false);

        saved.setSubtotal(totals[0]);
        saved.setTaxAmount(totals[1]);
        saved.setTotalAmount(totals[0].add(totals[1]));
        return toResponse(invoiceRepository.save(saved));
    }

    @Transactional
    public InvoiceResponse update(UUID id, InvoiceRequest req) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Chỉ cập nhật được hóa đơn ở trạng thái draft");
        }

        // -- Cập nhật header --
        Partner partner = new Partner(); partner.setId(req.getPartnerId());
        invoice.setPartner(partner);
        invoice.setInvoiceNumber(req.getInvoiceNumber());
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setDueDate(req.getDueDate());
        if (req.getCurrencyCode() != null) invoice.setCurrencyCode(req.getCurrencyCode());
        if (req.getExchangeRate() != null) invoice.setExchangeRate(req.getExchangeRate());

        // -- Xoá lines cũ không còn trong request rồi upsert --
        java.util.Set<UUID> incomingIds = req.getLines().stream()
                .map(InvoiceLineRequest::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<InvoiceLine> toDelete = invoiceLineRepository.findByInvoiceIdOrderByIdAsc(id).stream()
                .filter(l -> !incomingIds.contains(l.getId()))
                .toList();
        invoiceLineRepository.deleteAll(toDelete);

        BigDecimal[] totals = saveLines(invoice, req.getLines(), true);
        invoice.setSubtotal(totals[0]);
        invoice.setTaxAmount(totals[1]);
        invoice.setTotalAmount(totals[0].add(totals[1]));

        return toResponse(invoiceRepository.save(invoice));
    }

    /**
     * Xác nhận hóa đơn: draft → open.
     * Nếu là AP Bill, đặt approval_status = pending (chờ kế toán trưởng duyệt).
     * Nếu là AR Invoice, không cần phê duyệt.
     */
    @Transactional
    public InvoiceResponse confirm(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái draft");
        }
        invoice.setStatus("open");

        if ("AP".equals(invoice.getInvoiceType())) {
            invoice.setApprovalStatus("pending");
            log.info("✅ AP Bill [{}] đã được confirm. Trạng thái phê duyệt: pending", id);
        }

        return toResponse(invoiceRepository.save(invoice));
    }

    /**
     * Phê duyệt hóa đơn AP (approval_status: pending → approved).
     * Cập nhật DB trực tiếp, không qua Camunda.
     * Đồng thời trigger sinh JournalEntry tự động.
     */
    @Transactional
    public InvoiceResponse approve(UUID id, String comment) {
        Invoice invoice = findOrThrow(id);
        if (!"open".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái open để phê duyệt");
        }
        if (!"pending".equals(invoice.getApprovalStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái approval_status=pending để phê duyệt");
        }

        invoice.setApprovalStatus("approved");
        if (comment != null && !comment.isBlank()) {
            invoice.setApprovalComment(comment);
        }

        // Load đầy đủ Partner với apAccount (lazy fetch cần eager load thủ công)
        Partner partner = partnerRepository.findById(invoice.getPartner().getId())
                .orElseThrow(() -> new EntityNotFoundException("Đối tác không tồn tại: " + invoice.getPartner().getId()));
        invoice.setPartner(partner);

        // Load danh sách invoice lines
        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdOrderByIdAsc(id);

        // Sinh bút toán kép tự động
        JournalEntry journalEntry = journalService.createFromInvoice(invoice, lines);
        invoice.setJournalEntry(journalEntry);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("✅ AP Bill [{}] ĐÃ ĐƯỢC PHÊ DUYỆT. approval_status=approved, journalEntryId={}", id, journalEntry.getId());
        return toResponse(saved);
    }

    /**
     * Từ chối hóa đơn AP (approval_status: pending → rejected).
     * Cập nhật DB trực tiếp, không qua Camunda.
     */
    @Transactional
    public InvoiceResponse reject(UUID id, String comment) {
        Invoice invoice = findOrThrow(id);
        if (!"open".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái open để từ chối");
        }
        if (!"pending".equals(invoice.getApprovalStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái approval_status=pending để từ chối");
        }

        invoice.setApprovalStatus("rejected");
        if (comment != null && !comment.isBlank()) {
            invoice.setApprovalComment(comment);
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("❌ AP Bill [{}] ĐÃ BỊ TỪ CHỐI. approval_status=rejected", id);
        return toResponse(saved);
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        Invoice invoice = findOrThrow(id);
        invoice.setStatus("cancelled");
        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void delete(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Chỉ xoá được hóa đơn draft");
        }
        invoiceRepository.delete(invoice);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Danh sách hóa đơn AP đang chờ phê duyệt (approval_status = pending).
     */
    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getPendingApprovals(UUID companyId, int page, int size) {
        Page<Invoice> data = invoiceRepository.search(companyId, "AP", "open", null, PageRequest.of(page, size));
        List<InvoiceResponse> content = data.getContent().stream()
                .filter(inv -> "pending".equals(inv.getApprovalStatus()))
                .map(this::toResponse)
                .toList();
        return PagedResponse.of(content, page, size, content.size());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdue(UUID companyId) {
        return invoiceRepository.findOverdue(companyId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByPartner(UUID partnerId) {
        return invoiceRepository.findByPartnerIdOrderByInvoiceDateDesc(partnerId)
                .stream().map(this::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice findOrThrow(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hóa đơn không tồn tại"));
    }

    private InvoiceResponse toResponse(Invoice inv) {
        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceType(inv.getInvoiceType())
                .invoiceNumber(inv.getInvoiceNumber())
                .partnerId(inv.getPartner().getId())
                .partnerName(inv.getPartner().getName())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .currencyCode(inv.getCurrencyCode())
                .exchangeRate(inv.getExchangeRate())
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .paidAmount(inv.getPaidAmount())
                .status(inv.getStatus())
                .approvalStatus(inv.getApprovalStatus())
                .approvalComment(inv.getApprovalComment())
                .createdBy(inv.getCreatedBy() != null ? inv.getCreatedBy() : null)
                .createdAt(inv.getCreatedAt())
                .journalEntryId(inv.getJournalEntry() != null ? inv.getJournalEntry().getId() : null)
                .build();
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return InvoiceLineResponse.builder()
                .id(line.getId())
                .accountId(line.getAccount().getId())
                .accountCode(line.getAccount().getCode())
                .accountName(line.getAccount().getName())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .taxRate(line.getTaxRate())
                .taxAmount(line.getTaxAmount())
                .amount(line.getAmount())
                .build();
    }

    /**
     * Lưu/cập nhật danh sách lines cho một invoice.
     * @param invoice    Invoice entity đã được persist
     * @param lineReqs   Danh sách line request từ client
     * @param allowUpsert true → cho phép update line có id; false → luôn tạo mới (dùng cho create)
     * @return BigDecimal[]{subtotal, taxTotal}
     */
    private BigDecimal[] saveLines(Invoice invoice, List<InvoiceLineRequest> lineReqs, boolean allowUpsert) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (InvoiceLineRequest lineReq : lineReqs) {
            BigDecimal lineAmount = lineReq.getQuantity().multiply(lineReq.getUnitPrice());
            BigDecimal lineTax    = lineAmount.multiply(lineReq.getTaxRate()).divide(new BigDecimal("100"));

            InvoiceLine line;
            if (allowUpsert && lineReq.getId() != null) {
                line = invoiceLineRepository.findById(lineReq.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Invoice line không tồn tại: " + lineReq.getId()));
            } else {
                line = new InvoiceLine();
                line.setInvoice(invoice);
            }

            Account acc = new Account(); acc.setId(lineReq.getAccountId());
            line.setAccount(acc);
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setTaxRate(lineReq.getTaxRate());
            line.setAmount(lineAmount);
            line.setTaxAmount(lineTax);
            invoiceLineRepository.save(line);

            subtotal = subtotal.add(lineAmount);
            taxTotal = taxTotal.add(lineTax);
        }

        return new BigDecimal[]{subtotal, taxTotal};
    }
}
