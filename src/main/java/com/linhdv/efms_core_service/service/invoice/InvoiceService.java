package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.dto.invoice.request.CreateInvoiceRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceLineResponse;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.repository.invoice.InvoiceLineRepository;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;

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

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest req) {
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
        
        // Cập nhật sau
        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);

        Invoice saved = invoiceRepository.save(invoice);

        BigDecimal currentSubtotal = BigDecimal.ZERO;
        BigDecimal currentTax      = BigDecimal.ZERO;

        for (var lineReq : req.getLines()) {
            InvoiceLine line = new InvoiceLine();
            line.setInvoice(saved);
            
            Account acc = new Account(); acc.setId(lineReq.getAccountId());
            line.setAccount(acc);
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setTaxRate(lineReq.getTaxRate());
            
            BigDecimal lineAmount = lineReq.getQuantity().multiply(lineReq.getUnitPrice());
            BigDecimal lineTax    = lineAmount.multiply(lineReq.getTaxRate()).divide(new BigDecimal("100"));
            
            line.setTaxAmount(lineTax);
            line.setAmount(lineAmount);
            invoiceLineRepository.save(line);

            currentSubtotal = currentSubtotal.add(lineAmount);
            currentTax      = currentTax.add(lineTax);
        }

        saved.setSubtotal(currentSubtotal);
        saved.setTaxAmount(currentTax);
        saved.setTotalAmount(currentSubtotal.add(currentTax));

        return toResponse(invoiceRepository.save(saved));
    }

    @Transactional
    public InvoiceResponse confirm(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái draft");
        }
        invoice.setStatus("open");
        return toResponse(invoiceRepository.save(invoice));
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

    // AR/AP Aging: Lấy hoá đơn quá hạn
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdue(UUID companyId) {
        return invoiceRepository.findOverdue(companyId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    // Lịch sử hóa đơn đối tác
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByPartner(UUID partnerId) {
        return invoiceRepository.findByPartnerIdOrderByInvoiceDateDesc(partnerId)
                .stream().map(this::toResponse).toList();
    }

    // ── Helper ────────────────────────────────────────────────────────────────
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
}
