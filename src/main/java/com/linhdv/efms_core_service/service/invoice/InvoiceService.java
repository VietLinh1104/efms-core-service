package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.entity.*;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceRequest;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceLineRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceLineResponse;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.repository.invoice.InvoiceLineRepository;
import com.linhdv.efms_core_service.repository.invoice.InvoiceRepository;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import io.camunda.client.CamundaClient;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final CamundaClient camundaClient;
    private final com.linhdv.efms_core_service.service.camunda.TasklistApiClient tasklistApiClient;

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

    @Transactional
    public InvoiceResponse confirm(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"draft".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái draft");
        }
        invoice.setStatus("open");

        if ("AP".equals(invoice.getInvoiceType())) {
            invoice.setApprovalStatus("pending");

            var result = camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId("ap-bill-approval")
                    .latestVersion()
                    .variables(Map.of(
                            "invoiceId", invoice.getId().toString(),
                            "companyId", invoice.getCompanyId().toString(),
                            "partnerId", invoice.getPartner().getId().toString(),
                            "totalAmount", invoice.getTotalAmount().doubleValue(),
                            "submittedBy", invoice.getCreatedBy() != null ? invoice.getCreatedBy().toString() : ""
                    ))
                    .send().join();

            invoice.setCamundaProcessId(String.valueOf(result.getProcessInstanceKey()));
        }

        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        Invoice invoice = findOrThrow(id);
        invoice.setStatus("cancelled");
        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse approve(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"open".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái open để phê duyệt");
        }
        
        String taskId = tasklistApiClient.findTaskIdByProcessInstanceKey(invoice.getCamundaProcessId());
        if (taskId != null) {
            tasklistApiClient.completeTask(taskId, true, "Approved via InvoiceService");
            // Status IS NOT updated here. Camunda JobWorker will update the DB.
        } else {
            throw new RuntimeException("Không tìm thấy task phê duyệt trên Camunda cho process: " + invoice.getCamundaProcessId());
        }
        
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse reject(UUID id) {
        Invoice invoice = findOrThrow(id);
        if (!"open".equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái open để từ chối");
        }

        String taskId = tasklistApiClient.findTaskIdByProcessInstanceKey(invoice.getCamundaProcessId());
        if (taskId != null) {
            tasklistApiClient.completeTask(taskId, false, "Rejected via InvoiceService");
            // Status IS NOT updated here. Camunda JobWorker will update the DB.
        } else {
            throw new RuntimeException("Không tìm thấy task phê duyệt trên Camunda cho process: " + invoice.getCamundaProcessId());
        }

        return toResponse(invoice);
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

    @Transactional(readOnly = true)
    public InvoiceResponse getByCamundaProcessId(String processId) {
        return invoiceRepository.findByCamundaProcessId(processId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getAllApprovalTasks(int page, int size) {
        List<Map<String, Object>> tasks = tasklistApiClient.searchAllCreatedTasks();
        List<InvoiceResponse> responses = new java.util.ArrayList<>();
        if (tasks != null) {
            for (Map<String, Object> task : tasks) {
                String processInstanceKey = String.valueOf(task.get("processInstanceKey"));
                Invoice invoice = invoiceRepository.findByCamundaProcessId(processInstanceKey).orElse(null);
                if (invoice != null) {
                    InvoiceResponse resp = toResponse(invoice);
                    resp.setTaskId(String.valueOf(task.get("id")));
                    resp.setTaskName(String.valueOf(task.get("name")));
                    responses.add(resp);
                }
            }
        }
        
        int totalElements = responses.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        
        List<InvoiceResponse> pagedResponses = new java.util.ArrayList<>();
        if (fromIndex <= totalElements) {
            pagedResponses = responses.subList(fromIndex, toIndex);
        }
        
        return PagedResponse.of(pagedResponses, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceTaskDetail(String taskId) {
        Map<String, Object> task = tasklistApiClient.getTaskInfo(taskId);
        if (task == null || !task.containsKey("processInstanceKey")) {
            throw new EntityNotFoundException("Task not found or processInstanceKey missing");
        }
        String processInstanceKey = String.valueOf(task.get("processInstanceKey"));
        Invoice invoice = invoiceRepository.findByCamundaProcessId(processInstanceKey)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found for process: " + processInstanceKey));

        InvoiceResponse resp = toResponse(invoice);
        resp.setTaskId(String.valueOf(task.get("id")));
        resp.setTaskName(String.valueOf(task.get("name")));
        return resp;
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
                .approvalStatus(inv.getApprovalStatus())
                .camundaProcessId(inv.getCamundaProcessId())
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
