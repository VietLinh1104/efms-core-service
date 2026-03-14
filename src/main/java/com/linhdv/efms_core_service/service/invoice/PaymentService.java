package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.dto.invoice.request.CreatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.request.AllocatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoicePaymentResponse;
import com.linhdv.efms_core_service.dto.invoice.response.PaymentResponse;
import com.linhdv.efms_core_service.repository.invoice.InvoicePaymentRepository;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import com.linhdv.efms_core_service.repository.invoice.PaymentRepository;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> search(UUID companyId, String type, UUID partnerId, int page, int size) {
        Page<Payment> data = paymentRepository.search(companyId, type, partnerId, PageRequest.of(page, size));
        List<PaymentResponse> content = data.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getDetail(UUID id) {
        Payment p = findOrThrow(id);
        List<InvoicePaymentResponse> allocs = invoicePaymentRepository.findByPaymentIdOrderByIdAsc(id)
                .stream().map(this::toAllocResponse).toList();

        PaymentResponse resp = toResponse(p);
        resp.setAllocations(allocs);
        return resp;
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest req) {
        Company com = new Company(); com.setId(req.getCompanyId());
        Partner prt = new Partner(); prt.setId(req.getPartnerId());

        Payment p = new Payment();
        p.setCompany(com);
        p.setPartner(prt);
        p.setPaymentType(req.getPaymentType());
        p.setPaymentDate(req.getPaymentDate());
        p.setCurrencyCode(req.getCurrencyCode() != null ? req.getCurrencyCode() : "VND");
        p.setExchangeRate(req.getExchangeRate() != null ? req.getExchangeRate() : BigDecimal.ONE);
        p.setAmount(req.getAmount());
        p.setPaymentMethod(req.getPaymentMethod());
        p.setReference(req.getReference());
        p.setCreatedAt(Instant.now());

        if (req.getBankAccountId() != null) {
            BankAccount ba = new BankAccount(); ba.setId(req.getBankAccountId());
            p.setBankAccount(ba);
        }

        return toResponse(paymentRepository.save(p));
    }

    // Allocate payment to invoice
    @Transactional
    public PaymentResponse allocate(UUID paymentId, AllocatePaymentRequest req) {
        Payment payment = findOrThrow(paymentId);
        Invoice invoice = invoiceRepository.findById(req.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("Hóa đơn không tồn tại"));

        if (!"open".equals(invoice.getStatus()) && !"in_payment".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải đang mở để phân bổ");
        }

        BigDecimal pending = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (req.getAmount().compareTo(pending) > 0) {
            throw new IllegalArgumentException("Số tiền phân bổ vượt quá công nợ hóa đơn (" + pending + ")");
        }

        // Tạo dòng phân bổ
        InvoicePayment ip = new InvoicePayment();
        ip.setInvoice(invoice);
        ip.setPayment(payment);
        ip.setAllocatedAmount(req.getAmount());
        ip.setCreatedAt(Instant.now());
        invoicePaymentRepository.save(ip);

        // Cập nhật paid_amount của invoice
        invoice.setPaidAmount(invoice.getPaidAmount().add(req.getAmount()));
        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus("paid");
        } else {
            invoice.setStatus("in_payment");
        }
        invoiceRepository.save(invoice);

        return getDetail(paymentId);
    }

    @Transactional
    public void delete(UUID id) {
        Payment p = findOrThrow(id);
        if (p.getJournalEntry() != null) {
            throw new IllegalStateException("Không thể xoá thanh toán đã post bút toán");
        }
        paymentRepository.delete(p);
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private Payment findOrThrow(UUID id) {
        return paymentRepository.findById(id).orElseThrow();
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentType(p.getPaymentType())
                .partnerId(p.getPartner().getId())
                .partnerName(p.getPartner().getName())
                .paymentDate(p.getPaymentDate())
                .currencyCode(p.getCurrencyCode())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .reference(p.getReference())
                .createdBy(p.getCreatedBy() != null ? p.getCreatedBy().getName() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private InvoicePaymentResponse toAllocResponse(InvoicePayment ip) {
        return InvoicePaymentResponse.builder()
                .id(ip.getId())
                .paymentId(ip.getPayment().getId())
                .invoiceNumber(ip.getInvoice().getInvoiceNumber())
                .paymentDate(ip.getPayment().getPaymentDate())
                .allocatedAmount(ip.getAllocatedAmount())
                .createdAt(ip.getCreatedAt())
                // .createdBy(...) (Bỏ qua do DB tạm chưa có createdBy vào bảng invoice_payments)
                .build();
    }
}
