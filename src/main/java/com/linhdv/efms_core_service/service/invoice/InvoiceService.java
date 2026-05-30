package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceRequest;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceLineRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.mapper.invoice.InvoiceMapper;
import com.linhdv.efms_core_service.repository.invoice.InvoiceLineRepository;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import com.linhdv.efms_core_service.repository.invoice.PartnerRepository;
import com.linhdv.efms_core_service.service.AuditService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    private static final String TABLE = "invoices";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final PartnerRepository partnerRepository;
    private final JournalService journalService;
    private final AuditService auditService;
    private final InvoiceMapper invoiceMapper;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> search(UUID companyId, String type, String status, UUID partnerId, int page, int size) {
        Page<Invoice> data = invoiceRepository.search(companyId, type, status, partnerId, PageRequest.of(page, size));
        List<InvoiceResponse> content = data.getContent().stream().map(invoiceMapper::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getDetail(UUID id) {
        Invoice invoice = findOrThrow(id);
        var lines = invoiceLineRepository.findByInvoiceIdOrderByIdAsc(id)
                .stream().map(invoiceMapper::toLineResponse).toList();

        InvoiceResponse resp = invoiceMapper.toResponse(invoice);
        resp.setLines(lines);
        return resp;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest req) {
        Invoice invoice = invoiceMapper.toEntity(req);

        Invoice saved = invoiceRepository.save(invoice);
        BigDecimal[] totals = saveLines(saved, req.getLines(), false);

        saved.setSubtotal(totals[0]);
        saved.setTaxAmount(totals[1]);
        saved.setTotalAmount(totals[0].add(totals[1]));
        Invoice result = invoiceRepository.save(saved);
        auditService.log(TABLE, result.getId(), "INSERT", null, auditService.toMap(result));
        return invoiceMapper.toResponse(result);
    }

    @Transactional
    public InvoiceResponse update(UUID id, InvoiceRequest req) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Chỉ cập nhật được hóa đơn ở trạng thái draft");
        }

        // -- Cập nhật header --
        invoiceMapper.updateEntityFromRequest(req, invoice);

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

        Map<String, Object> oldSnapshot = auditService.toMap(findOrThrow(id));
        Invoice result = invoiceRepository.save(invoice);
        auditService.log(TABLE, result.getId(), "UPDATE", oldSnapshot, auditService.toMap(result));
        return invoiceMapper.toResponse(result);
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

        Map<String, Object> oldSnapshot = Map.of("status", "draft");
        invoice.setStatus("open");

        if ("AP".equals(invoice.getInvoiceType())) {
            invoice.setApprovalStatus("pending");
            log.info("✅ AP Bill [{}] đã được confirm. Trạng thái phê duyệt: pending", id);
        }

        Invoice result = invoiceRepository.save(invoice);
        auditService.log(TABLE, id, "CONFIRM", oldSnapshot,
                Map.of("status", "open", "approvalStatus",
                        result.getApprovalStatus() != null ? result.getApprovalStatus() : ""));
        return invoiceMapper.toResponse(result);
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
        auditService.log(TABLE, id, "APPROVE",
                Map.of("approvalStatus", "pending"),
                Map.of("approvalStatus", "approved",
                        "comment", comment != null ? comment : "",
                        "journalEntryId", journalEntry.getId().toString()));
        log.info("✅ AP Bill [{}] ĐÃ ĐƯỢC PHÊ DUYỆT. approval_status=approved, journalEntryId={}", id, journalEntry.getId());
        return invoiceMapper.toResponse(saved);
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
        auditService.log(TABLE, id, "REJECT",
                Map.of("approvalStatus", "pending"),
                Map.of("approvalStatus", "rejected",
                        "comment", comment != null ? comment : ""));
        log.info("❌ AP Bill [{}] ĐÃ BỊ TỪ CHỐI. approval_status=rejected", id);
        return invoiceMapper.toResponse(saved);
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        Invoice invoice = findOrThrow(id);
        Map<String, Object> oldSnapshot = Map.of("status", invoice.getStatus());
        invoice.setStatus("cancelled");
        Invoice result = invoiceRepository.save(invoice);
        auditService.log(TABLE, id, "CANCEL", oldSnapshot, Map.of("status", "cancelled"));
        return invoiceMapper.toResponse(result);
    }

    @Transactional
    public void delete(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Chỉ xoá được hóa đơn draft");
        }
        Map<String, Object> oldSnapshot = auditService.toMap(invoice);
        invoiceRepository.delete(invoice);
        auditService.log(TABLE, id, "DELETE", oldSnapshot, null);
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
                .map(invoiceMapper::toResponse)
                .toList();
        return PagedResponse.of(content, page, size, content.size());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdue(UUID companyId) {
        return invoiceRepository.findOverdue(companyId, LocalDate.now())
                .stream().map(invoiceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByPartner(UUID partnerId) {
        return invoiceRepository.findByPartnerIdOrderByInvoiceDateDesc(partnerId)
                .stream().map(invoiceMapper::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice findOrThrow(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hóa đơn không tồn tại"));
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
                line = invoiceMapper.toLineEntity(lineReq);
                line.setInvoice(invoice);
            }

            if (allowUpsert && lineReq.getId() != null) {
                invoiceMapper.updateLineFromRequest(lineReq, line);
            }
            line.setAmount(lineAmount);
            line.setTaxAmount(lineTax);
            invoiceLineRepository.save(line);

            subtotal = subtotal.add(lineAmount);
            taxTotal = taxTotal.add(lineTax);
        }

        return new BigDecimal[]{subtotal, taxTotal};
    }
}
