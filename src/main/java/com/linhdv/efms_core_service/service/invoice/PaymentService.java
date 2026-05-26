package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.dto.invoice.request.CreatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PaymentResponse;
import com.linhdv.efms_core_service.repository.finance.BankAccountRepository;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import com.linhdv.efms_core_service.repository.invoice.PartnerRepository;
import com.linhdv.efms_core_service.repository.invoice.PaymentRepository;
import com.linhdv.efms_core_service.repository.accounting.JournalEntryRepository;
import com.linhdv.efms_core_service.repository.accounting.JournalLineRepository;
import com.linhdv.efms_core_service.service.AuditService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TABLE = "payments";

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PartnerRepository partnerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> search(UUID companyId, String type, UUID partnerId, int page, int size) {
        Page<Payment> data = paymentRepository.search(companyId, type, partnerId, PageRequest.of(page, size));
        List<PaymentResponse> content = data.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getDetail(UUID id) {
        Payment p = findOrThrow(id);
        return toResponse(p);
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest req) {
        Partner prt = new Partner(); prt.setId(req.getPartnerId());

        Payment p = new Payment();
        p.setCompanyId(req.getCompanyId());
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

        Invoice invoice = null;
        if (req.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(req.getInvoiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Hóa đơn không tồn tại"));

            if (!"open".equals(invoice.getStatus()) && !"in_payment".equals(invoice.getStatus())) {
                throw new IllegalStateException("Hóa đơn phải đang mở để thanh toán");
            }

            String expectedType = "in".equalsIgnoreCase(req.getPaymentType()) ? "AR" : "AP";
            if (!expectedType.equals(invoice.getInvoiceType())) {
                throw new IllegalStateException("Loại hóa đơn không khớp với loại giao dịch thanh toán (Thu phải đi với AR, Chi phải đi với AP)");
            }

            if ("AP".equals(invoice.getInvoiceType()) && !"approved".equals(invoice.getApprovalStatus())) {
                throw new IllegalStateException("Hóa đơn mua hàng (AP Bill) phải được phê duyệt trước khi thanh toán");
            }

            BigDecimal pending = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
            if (req.getAmount().compareTo(pending) > 0) {
                throw new IllegalArgumentException("Số tiền thanh toán vượt quá công nợ hóa đơn (" + pending + ")");
            }

            p.setInvoice(invoice);

            // Cập nhật paid_amount của invoice
            invoice.setPaidAmount(invoice.getPaidAmount().add(req.getAmount()));
            if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus("paid");
            } else {
                invoice.setStatus("in_payment");
            }
            invoiceRepository.save(invoice);
        }

        Payment result = paymentRepository.save(p);

        if (invoice != null) {
            auditService.log("invoices", invoice.getId(), "PAYMENT_ALLOCATE",
                    Map.of("paidAmount", invoice.getPaidAmount().subtract(req.getAmount()).toPlainString(), "status", "open"),
                    Map.of("paidAmount", invoice.getPaidAmount().toPlainString(),
                            "status", invoice.getStatus(),
                            "paymentId", result.getId().toString(),
                            "allocatedAmount", req.getAmount().toPlainString()));
        }

        auditService.log(TABLE, result.getId(), "INSERT", null, auditService.toMap(result));
        return toResponse(result);
    }

    /**
     * Ghi sổ (Post) một Payment vào General Ledger.
     *
     * Quy tắc double-entry:
     *  - Payment OUT (Chi tiền): Nợ TK AP đối tác / Có TK Tiền (GL của BankAccount)
     *  - Payment IN  (Thu tiền): Nợ TK Tiền (GL của BankAccount) / Có TK AR đối tác
     *
     * Nếu payment dùng phương thức "cash" (không có bankAccount),
     * hệ thống sẽ dùng TK mặc định của đối tác làm tài khoản tiền.
     */
    @Transactional
    public PaymentResponse post(UUID id) {
        Payment payment = findOrThrow(id);

        if (payment.getJournalEntry() != null) {
            throw new IllegalStateException("Thanh toán này đã được ghi sổ (journalEntryId=" + payment.getJournalEntry().getId() + ")");
        }

        // Load đầy đủ đối tác (cần arAccount / apAccount)
        Partner partner = partnerRepository.findById(payment.getPartner().getId())
                .orElseThrow(() -> new EntityNotFoundException("Đối tác không tồn tại: " + payment.getPartner().getId()));

        // Xác định TK Tiền (cash/bank GL account)
        Account cashAccount;
        if (payment.getBankAccount() != null) {
            BankAccount bankAccount = bankAccountRepository.findById(payment.getBankAccount().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Tài khoản ngân hàng không tồn tại: " + payment.getBankAccount().getId()));
            cashAccount = bankAccount.getGlAccount();
            if (cashAccount == null) {
                throw new IllegalStateException("Tài khoản ngân hàng [" + bankAccount.getId() + "] chưa cấu hình GL Account (gl_account_id).");
            }
        } else {
            // Thanh toán tiền mặt: dùng arAccount làm TK tiền nếu thu, apAccount nếu chi
            // (thực tế nên có TK tiền mặt riêng — đây là fallback khi không chọn bank_account)
            throw new IllegalStateException(
                    "Phương thức thanh toán tiền mặt (cash) yêu cầu phải chọn Tài khoản Ngân hàng/Tiền mặt (bank_account_id) để xác định GL Account.");
        }

        boolean isOut = "out".equalsIgnoreCase(payment.getPaymentType());

        // Xác định TK đối ứng phía đối tác
        Account partnerAccount;
        if (isOut) {
            partnerAccount = partner.getApAccount();
            if (partnerAccount == null) {
                throw new IllegalStateException("Đối tác [" + partner.getId() + "] chưa cấu hình TK phải trả (ap_account_id).");
            }
        } else {
            partnerAccount = partner.getArAccount();
            if (partnerAccount == null) {
                throw new IllegalStateException("Đối tác [" + partner.getId() + "] chưa cấu hình TK phải thu (ar_account_id).");
            }
        }

        String currency = payment.getCurrencyCode() != null ? payment.getCurrencyCode() : "VND";
        BigDecimal exchangeRate = payment.getExchangeRate() != null ? payment.getExchangeRate() : BigDecimal.ONE;
        BigDecimal amount = payment.getAmount();

        // 1. Tạo Journal Entry header
        JournalEntry je = new JournalEntry();
        je.setCompanyId(payment.getCompanyId());
        je.setEntryDate(payment.getPaymentDate());
        je.setReference(payment.getReference() != null ? payment.getReference() : payment.getId().toString());
        je.setDescription((isOut ? "Chi tiền" : "Thu tiền") + " — " + partner.getName()
                + (payment.getReference() != null ? " / " + payment.getReference() : ""));
        je.setStatus("posted");
        je.setSource("payment");
        je.setSourceRefId(payment.getId());
        je.setCreatedBy(payment.getCreatedBy());
        je.setCreatedAt(Instant.now());
        JournalEntry savedJe = journalEntryRepository.save(je);

        // 2. Dòng Nợ
        Account debitAccount  = isOut ? partnerAccount : cashAccount;
        // 3. Dòng Có
        Account creditAccount = isOut ? cashAccount : partnerAccount;

        JournalLine debitLine = new JournalLine();
        debitLine.setJournalEntry(savedJe);
        debitLine.setAccount(debitAccount);
        debitLine.setPartner(partner);
        debitLine.setDebit(amount);
        debitLine.setCredit(BigDecimal.ZERO);
        debitLine.setCurrencyCode(currency);
        debitLine.setAmountCurrency(amount);
        debitLine.setExchangeRate(exchangeRate);
        debitLine.setDescription(isOut ? "Chi trả công nợ: " + partner.getName() : "Thu tiền: " + partner.getName());
        debitLine.setCreatedAt(Instant.now());
        journalLineRepository.save(debitLine);

        JournalLine creditLine = new JournalLine();
        creditLine.setJournalEntry(savedJe);
        creditLine.setAccount(creditAccount);
        creditLine.setPartner(partner);
        creditLine.setDebit(BigDecimal.ZERO);
        creditLine.setCredit(amount);
        creditLine.setCurrencyCode(currency);
        creditLine.setAmountCurrency(amount);
        creditLine.setExchangeRate(exchangeRate);
        creditLine.setDescription(isOut ? "Xuất quỹ: " + partner.getName() : "Nhập quỹ/ngân hàng: " + partner.getName());
        creditLine.setCreatedAt(Instant.now());
        journalLineRepository.save(creditLine);

        // 4. Gắn Journal Entry vào Payment và lưu
        payment.setJournalEntry(savedJe);
        Payment saved = paymentRepository.save(payment);

        auditService.log(TABLE, id, "POST",
                Map.of("journalEntryId", "null"),
                Map.of("journalEntryId", savedJe.getId().toString()));
        log.info("✅ Payment [{}] ĐÃ ĐƯỢC GHI SỔ. journalEntryId={}", id, savedJe.getId());
        return getDetail(saved.getId());
    }

    @Transactional
    public void delete(UUID id) {
        Payment p = findOrThrow(id);
        if (p.getJournalEntry() != null) {
            throw new IllegalStateException("Không thể xoá thanh toán đã post bút toán");
        }

        Invoice invoice = p.getInvoice();
        if (invoice != null) {
            Map<String, Object> invoiceOld = Map.of(
                    "paidAmount", invoice.getPaidAmount().toPlainString(),
                    "status", invoice.getStatus());

            BigDecimal updatedPaid = invoice.getPaidAmount().subtract(p.getAmount());
            if (updatedPaid.compareTo(BigDecimal.ZERO) < 0) {
                updatedPaid = BigDecimal.ZERO;
            }
            invoice.setPaidAmount(updatedPaid);

            if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus("open");
            } else if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus("paid");
            } else {
                invoice.setStatus("in_payment");
            }
            invoiceRepository.save(invoice);

            auditService.log("invoices", invoice.getId(), "PAYMENT_REMOVE", invoiceOld,
                    Map.of("paidAmount", invoice.getPaidAmount().toPlainString(),
                            "status", invoice.getStatus(),
                            "paymentId", p.getId().toString()));
        }

        Map<String, Object> oldSnapshot = auditService.toMap(p);
        paymentRepository.delete(p);
        auditService.log(TABLE, id, "DELETE", oldSnapshot, null);
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
                .createdBy(p.getCreatedBy() != null ? p.getCreatedBy() : null)
                .createdAt(p.getCreatedAt())
                .journalEntryId(p.getJournalEntry() != null ? p.getJournalEntry().getId() : null)
                .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                .invoiceNumber(p.getInvoice() != null ? p.getInvoice().getInvoiceNumber() : null)
                .build();
    }
}
